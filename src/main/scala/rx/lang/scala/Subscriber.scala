/**
 * Copyright 2014 Netflix, Inc.
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

/**
 * An extension of the [[Observer]] trait which adds subscription handling
 * ([[unsubscribe]], [[isUnsubscribed]], and `add` methods) and backpressure handling
 * ([[onStart]] and [[request]] methods).
 *
 * After a [[Subscriber]] calls an [[Observable]]'s `subscribe` method, the
 * [[Observable]] calls the [[Subscriber]]'s [[onNext]] method to emit items. A well-behaved
 * [[Observable]] will call a [[Subscriber]]'s [[onCompleted]] method exactly once or the [[Subscriber]]'s
 * [[onError]] method exactly once.
 * 
 * Similarly to the RxJava `Subscriber`, this class has two constructors:
 * 
 * The first constructor takes as argument the child [[Subscriber]] from further down the pipeline
 * and is usually only needed together with [[Observable.lift lift]]:
 * 
 * {{{
 * myObservable.lift((subscriber: Subscriber[T]) => new Subscriber[T](subscriber) {
 *   override def onStart(): Unit = ...
 *   override def onNext(n: T): Unit = ...
 *   override def onError(e: Throwable): Unit = ...
 *   override def onCompleted(): Unit = ...
 * })
 * }}}
 * 
 * The second constructor takes no arguments and is typically used with the subscribe method:
 * 
 * {{{
 * myObservable.subscribe(new Subscriber[T] {
 *   override def onStart(): Unit = ...
 *   override def onNext(n: T): Unit = ...
 *   override def onError(e: Throwable): Unit = ...
 *   override def onCompleted(): Unit = ...
 * })
 * }}}
 * 
 * Notice that these two constructors are not (as usually in Scala) in the companion object,
 * because if they were, we couldn't create anonymous classes implementing
 * `onStart`/`onNext`/`onError`/`onCompleted` as in the examples above.
 * However, there are more constructors in the companion object, which allow you to construct
 * Subscribers from given `onNext`/`onError`/`onCompleted` lambdas.
 */
abstract class Subscriber[-T](subscriber: Subscriber[_]) extends Observer[T] with Subscription {

  self =>

  def this() {
      this(null)
  }

  // Implementation note:
  // If the Subscriber() constructor was used, we have to call the rx.Subscriber() constructor 
  // to construct the asJavaSubscriber, and if the Subscriber(Subscriber) constructor was used, we have
  // to call the rx.Subscriber(rx.Subscriber) constructor.
  // The obvious way to achieve this would be to have a primary constructor which takes an
  // underlying rx.Subscriber, and add two secondary constructors, Subscriber() and Subscriber(Subscriber),
  // which call the primary constructor with the right rx.Subscriber.
  // But this does not work because a secondary constructor does not have access to self before it
  // calls the primary constructor, and we need `self` to create the rx.Subscriber.
  // So we have to construct the asJavaSubscriber in the primary constructor (=class body) of Subscriber,
  // and we model the Subscriber() constructor as Subscriber(null).
  // Notice that the apply(rx.Subscriber) constructor in the companion object will override this asJavaSubscriber.
  private[scala] val asJavaSubscriber: rx.Subscriber[_ >: T] =
    if (subscriber == null) {
      new rx.Subscriber[T] with SubscriberAdapter[T] {
        override def onStart(): Unit = self.onStart()

        override def onNext(value: T): Unit = self.onNext(value)

        override def onError(error: Throwable): Unit = self.onError(error)

        override def onCompleted(): Unit = self.onCompleted()

        override def requestMore(n: Long): Unit = request(n)
      }
    }
    else {
      new rx.Subscriber[T](subscriber.asJavaSubscriber) with SubscriberAdapter[T] {
        override def onStart(): Unit = self.onStart()

        override def onNext(value: T): Unit = self.onNext(value)

        override def onError(error: Throwable): Unit = self.onError(error)

        override def onCompleted(): Unit = self.onCompleted()

        override def requestMore(n: Long): Unit = request(n)
      }
    }

  private [scala] override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubscriber
  private [scala] override val asJavaSubscription: rx.Subscription = asJavaSubscriber

  /**
   * Add a [[Subscription]] to this [[Subscriber]]'s list of [[Subscription]]s if this list is not marked as
   * unsubscribed. If the list **is** marked as unsubscribed, it will unsubscribe the new [[Subscription]] as well.
   *
   * @param s the [[Subscription]] to add
   */
  final def add(s: Subscription): Unit = {
    asJavaSubscriber.add(s.asJavaSubscription)
  }

  /**
   * Create a [[Subscription]] using `u` and add it to this [[Subscriber]]'s list of [[Subscription]]s if this list
   * is not marked as unsubscribed. If the list **is** marked as unsubscribed, it will call `u`.
   *
   * @param u callback to run when unsubscribed
   */
  final def add(u: => Unit): Unit = {
    asJavaSubscriber.add(Subscription(u).asJavaSubscription)
  }

  /**
   * Unsubscribe all [[Subscription]]s added to this Subscriber's .
   */
  override final def unsubscribe(): Unit = {
    asJavaSubscriber.unsubscribe()
  }

  /**
   * Indicates whether this [[Subscriber]] has unsubscribed from its list of [[Subscription]]s.
   *
   * @return `true` if this [[Subscriber]] has unsubscribed from its [[Subscription]]s, `false` otherwise
   */
  override final def isUnsubscribed: Boolean = {
    asJavaSubscriber.isUnsubscribed
  }

  /**
   * This method is invoked when the [[Subscriber]] and [[Observable]] have been connected but the [[Observable]] has
   * not yet begun to emit items or send notifications to the [[Subscriber]]. Override this method to add any
   * useful initialization to your subscription, for instance to initiate backpressure.
   *
   * {{{
   * Observable.just(1, 2, 3).subscribe(new Subscriber[Int]() {
   *   override def onStart(): Unit = request(1)
   *   override def onNext(v: Int): Unit = {
   *     println(v)
   *     request(1)
   *   }
   *   override def onError(e: Throwable): Unit = e.printStackTrace()
   *   override def onCompleted(): Unit = {}
   * })
   * }}}
   */
  def onStart(): Unit = {
    // do nothing by default
  }

  /**
   * Request a certain maximum number of emitted items from the [[Observable]] this [[Subscriber]] is subscribed to.
   * This is a way of requesting backpressure. To disable backpressure, pass `Long.MaxValue` to this method.
   *
   * @param n the maximum number of items you want the [[Observable]] to emit to the [[Subscriber]] at this time, or
   *          `Long.MaxValue` if you want the [[Observable]] to emit items at its own pace
   */
  protected[this] final def request(n: Long): Unit = {
    asJavaSubscriber match {
      case s: SubscriberAdapter[T] => s.requestMore(n)
      case _ => throw new rx.exceptions.MissingBackpressureException()
    }
  }

  def setProducer(producer: Producer): Unit = {
    asJavaSubscriber.setProducer(producer.asJavaProducer)
  }

  def setProducer(producer: Long => Unit): Unit = {
    asJavaSubscriber.setProducer(Producer(producer).asJavaProducer)
  }
}

object Subscriber extends ObserverFactoryMethods[Subscriber] {

  private[scala] def apply[T](subscriber: rx.Subscriber[T]): Subscriber[T] = new Subscriber[T] {
    override val asJavaSubscriber = subscriber
    override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubscriber
    override val asJavaSubscription: rx.Subscription = asJavaSubscriber

    override def onStart(): Unit = asJavaSubscriber.onStart()
    override def onNext(value: T): Unit = asJavaSubscriber.onNext(value)
    override def onError(error: Throwable): Unit = asJavaSubscriber.onError(error)
    override def onCompleted(): Unit = asJavaSubscriber.onCompleted()

    // Calling the `request` method of this `Subscriber` will crash. Fortunately, the visibility of
    // `Subscriber.request` is `protected[this]`, users can only call it in Subscriber or its
    // subclasses. Therefore the `request` method of the Subscriber returned by `apply[T](subscriber: rx.Subscriber[T])`
    // won't be called. In addition, `scala.Observable` will pass `subscriber.asJavaSubscriber`
    // to `rx.Observable`, so if `rx.Subscriber` supports backpressure, it will still work.
  }

  def apply[T](onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Subscriber[T] = {
    val n = onNext; val e = onError; val c = onCompleted
    new Subscriber[T] {
      override def onNext(value: T): Unit = n(value)
      override def onError(error: Throwable): Unit = e(error)
      override def onCompleted(): Unit = c()
    }
  }

}

private[scala] sealed trait SubscriberAdapter[T] extends rx.Subscriber[T] {
  // Add a method to expose the protected `request` method
  def requestMore(n: Long): Unit
}
