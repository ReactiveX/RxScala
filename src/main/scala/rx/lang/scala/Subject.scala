/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import scala.language.implicitConversions

/**
* A Subject is an Observable and an Observer at the same time.
*/
trait Subject[T] extends Observable[T] with Observer[T] {
  private [scala] val asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]

  val asJavaObservable: rx.Observable[_ <: T] = asJavaSubject

  override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubject
  override def onNext(value: T): Unit = { asJavaObserver.onNext(value)}
  override def onError(error: Throwable): Unit = { asJavaObserver.onError(error)  }
  override def onCompleted() { asJavaObserver.onCompleted() }
}

/**
 * Subject that, once an `Observer` has subscribed, emits all subsequently observed items to the
 * subscriber.
 * <p>
 * <img width="640" height="405" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="">
 * <p>
 * @example
 {{{
  val subject = Subject[String]()
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1)
  subject.onNext("one")
  subject.onNext("two")
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2)
  subject.onNext("three")
  subject.onCompleted()
  }}}
 */
object Subject {
  /**
   * Creates and returns a new `Subject`.
   *
   * @return the new `Subject`
   */
  def apply[T](): Subject[T] = new rx.lang.scala.subjects.PublishSubject[T](rx.subjects.PublishSubject.create())

  implicit class SubjectExtensions[T](val subject: Subject[T]) extends AnyVal {

    def toSerialized: rx.lang.scala.subjects.SerializedSubject[T] = {
      subject match {
        case s: rx.lang.scala.subjects.SerializedSubject[T] => s
        case s => rx.lang.scala.subjects.SerializedSubject[T](s)
      }
    }

  }
}