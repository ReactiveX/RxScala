/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import scala.language.implicitConversions
import Decorators._

/**
 * Provides conversion functions `asJava` and `asScala` to convert
 * between RxScala types and RxJava types.
 *
 * Example:
 * {{{
 * import rx.lang.scala.JavaConverters._
 * val javaObs = Observable.just(1, 2, 3).asJava
 * val scalaObs = javaObs.asScala
 * }}}
 */
object JavaConverters extends DecorateAsJava with DecorateAsScala

private[scala] trait Decorators {

  class AsJava[A](op: => A) {
    def asJava: A = op
  }

  class AsJavaSubscription(s: Subscription) {
    def asJavaSubscription: rx.Subscription = s.asJavaSubscription
  }

  class AsJavaSubscriber[A](s: Subscriber[A]) {
    def asJavaSubscriber: rx.Subscriber[_ >: A] = s.asJavaSubscriber
  }

  class AsScala[A](op: => A) {
    def asScala: A = op
  }

  class AsScalaSubscription(s: rx.Subscription) {
    def asScalaSubscription: Subscription = Subscription(s)
  }

  class AsScalaSubscriber[A](s: rx.Subscriber[_ >: A]) {
    def asScalaSubscriber: Subscriber[A] = Subscriber(s)
  }
}

private[scala] object Decorators extends Decorators

/**
 * These functions convert RxScala types to RxJava types.
 * Pure Scala projects won't need them, but they will be useful for polyglot projects.
 */
trait DecorateAsJava {

  implicit def toJavaNotification[T](s: Notification[T]): AsJava[rx.Notification[_ <: T]] =
    new AsJava(s.asJavaNotification)

  implicit def toJavaSubscription(s: Subscription): AsJavaSubscription =
    new AsJavaSubscription(s)

  implicit def toJavaSubscriber[T](s: Subscriber[T]): AsJavaSubscriber[T] =
    new AsJavaSubscriber(s)

  implicit def toJavaScheduler(s: Scheduler): AsJava[rx.Scheduler] =
    new AsJava(s.asJavaScheduler)

  implicit def toJavaWorker(s: Worker): AsJava[rx.Scheduler.Worker] =
    new AsJava(s.asJavaWorker)

  implicit def toJavaObserver[T](s: Observer[T]): AsJava[rx.Observer[_ >: T]] =
    new AsJava(s.asJavaObserver)

  implicit def toJavaObservable[T](s: Observable[T]): AsJava[rx.Observable[_ <: T]] =
    new AsJava(s.asJavaObservable)

  private type jOperator[R, T] = rx.Observable.Operator[R, T]

  implicit def toJavaOperator[T, R](operator: Subscriber[R] => Subscriber[T]): AsJava[jOperator[R, T]] = {
    val jOp = new jOperator[R, T] {
      override def call(subscriber: rx.Subscriber[_ >: R]): rx.Subscriber[_ >: T] = {
        import JavaConverters.toScalaSubscriber
        operator(subscriber.asScalaSubscriber).asJavaSubscriber
      }
    }
    new AsJava[jOperator[R, T]](jOp)
  }
}

/**
 * These functions convert RxJava types to RxScala types.
 * Pure Scala projects won't need them, but they will be useful for polyglot projects.
 */
trait DecorateAsScala {

  implicit def toScalaNotification[T](s: rx.Notification[_ <: T]): AsScala[Notification[T]] =
    new AsScala(Notification(s))

  implicit def toScalaSubscription(s: rx.Subscription): AsScalaSubscription =
    new AsScalaSubscription(s)

  implicit def toScalaSubscriber[T](s: rx.Subscriber[_ >: T]): AsScalaSubscriber[T] =
    new AsScalaSubscriber(s)

  implicit def toScalaScheduler(s: rx.Scheduler): AsScala[Scheduler] =
    new AsScala(Scheduler(s))

  implicit def toScalaWorker(s: rx.Scheduler.Worker): AsScala[Worker] =
    new AsScala(Worker(s))

  implicit def toScalaObserver[T](s: rx.Observer[_ >: T]): AsScala[Observer[T]] =
    new AsScala(Observer(s))

  implicit def toScalaObservable[T](s: rx.Observable[_ <: T]): AsScala[Observable[T]] = {
    val obs = new Observable[T] {
      val asJavaObservable: rx.Observable[_ <: T] = s
    }
    new AsScala[Observable[T]](obs)
  }
}
