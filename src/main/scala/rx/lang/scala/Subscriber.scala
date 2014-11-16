package rx.lang.scala

/**
 * An extension of the `Observer` trait which adds subscription handling
 * (`unsubscribe`, `isUnsubscribed`, and `add` methods) and backpressure handling
 * (`onStart` and `request` methods).
 * 
 * Similarly to the RxJava Subscriber, this class has two constructors:
 * 
 * The first constructor takes as argument the child Subscriber from further down the pipeline
 * and is usually only needed together with `lift`:
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
   * Used to register an unsubscribe callback.
   */
  final def add(s: Subscription): Unit = {
    asJavaSubscriber.add(s.asJavaSubscription)
  }

  /**
   * Register a callback to be run when Subscriber is unsubscribed
   *
   * @param u callback to run when unsubscribed
   */
  final def add(u: => Unit): Unit = {
    asJavaSubscriber.add(Subscription(u).asJavaSubscription)
  }

  override final def unsubscribe(): Unit = {
    asJavaSubscriber.unsubscribe()
  }

  override final def isUnsubscribed: Boolean = {
    asJavaSubscriber.isUnsubscribed
  }

  def onStart(): Unit = {
    // do nothing by default
  }

  protected final def request(n: Long): Unit = {
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
