package rx.lang.scala.observables

import rx.annotations.Experimental
import rx.lang.scala.{Notification, Observable, Observer}

/**
  * An utility class to create observables that start acting when subscribed to and respond correctly
  * to back pressure requests from subscribers.
  */
object SyncOnSubscribe {
  /**
    * Generates an `Observable` that synchronously calls the provided `next` function
    * to generate data to downstream subscribers.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces data to the downstream subscriber
    * @param onUnsubscribe clean up behavior
    * @return An Observable that emits data downstream in a protocol compatible with back-pressure.
    * @see rx.observables.AsyncOnSubscribe.createStateful
    */
  @Experimental
  def createStateful[S, T](generator: () => S)(next: (S, Observer[_ >: T]) => S, onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    new SyncOnSubscribeImpl(generator, next, onUnsubscribe).toObservable

  /**
    * Generates an `Observable` that synchronously calls the provided `next` function
    * to generate data to downstream subscribers.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces data to the downstream subscriber
    * @param onUnsubscribe clean up behavior
    * @return An Observable that emits data downstream in a protocol compatible with back-pressure.
    * @see rx.observables.AsyncOnSubscribe.createSingleState
    */
  @Experimental
  def createSingleState[S,T](generator: () => S)(next: (S, Observer[_ >: T]) => Unit, onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    new SyncOnSubscribeImpl[S,T](generator, (s, obs) => { next(s, obs); s }, onUnsubscribe).toObservable

  /**
    * Generates an `Observable` that synchronously calls the provided `next` function
    * to generate data to downstream subscribers.
    *
    * @tparam T the type of the generated values
    * @param next produces data to the downstream subscriber
    * @param onUnsubscribe clean up behavior
    * @return An Observable that emits data downstream in a protocol compatible with back-pressure.
    * @see rx.observables.AsyncOnSubscribe.createStateless
    */
  @Experimental
  def createStateless[T](next: Observer[_ >: T] => Unit, onUnsubscribe: () => Unit = () => Unit): Observable[T] =
    new SyncOnSubscribeImpl[Unit, T](() => (), (_, obs) => next(obs), (_:Unit) => onUnsubscribe()).toObservable

  /**
    * Generates an Observable that calls the provided `next` function to generate data for the stream.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    * @return An observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def apply[S, T](generator: () => S)(next: S => (Notification[T], S), onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] = {
    val nextF: (S, Observer[_ >: T]) => S =
      (state, obs) => next(state) match {
        case (notification, nextState) =>
          notification.accept(obs)
          nextState
      }
    new SyncOnSubscribeImpl(generator, nextF, onUnsubscribe).toObservable
  }

  /**
    * Generates an Observable that calls the provided `next` function to generate data for the stream.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the state value
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    * @return An observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def applySingleState[S,T](generator: () => S)(next: S => Notification[T], onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    apply(generator)(s => (next(s),s), onUnsubscribe)

  /**
    * Generates an `Observable` that synchronously calls the provided `next` function to generate data for the stream.
    *
    * @tparam T the type of the generated values
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    * @return An Observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def applyStateless[T](next: () => Notification[T], onUnsubscribe: () => Unit = () => ()) =
    apply(() => ())(_ => (next(), ()), _ => onUnsubscribe())

  private[scala] class SyncOnSubscribeImpl[S,T](val generatorF: () => S, val nextF: (S, Observer[_ >: T]) => S, val onUnsubscribeF: S => Unit) {
    import rx.lang.scala.JavaConversions._

    private[scala] val asJavaSyncOnSubscribe = new rx.observables.SyncOnSubscribe[S,T] {
      override def generateState(): S = generatorF()
      override def next(state: S, observer: rx.Observer[_ >: T]): S = nextF(state, observer)
      override def onUnsubscribe(state: S): Unit = onUnsubscribeF(state)
    }

    def toObservable = toScalaObservable(rx.Observable.create(asJavaSyncOnSubscribe))
  }
}
