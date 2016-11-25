package rx.lang.scala.observables

import rx.annotations.Experimental
import rx.lang.scala.{Notification, Observable, Observer}

/**
  * An utility class to create observables that start acting when subscribed to and respond correctly
  * to back pressure requests from subscribers.
  */
object AsyncOnSubscribe {
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
  def createStateful[S, T](generator: () => S)(next: (S, Long, Observer[Observable[_ <: T]]) => S, onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    new AsyncOnSubscribeImpl[S, T](generator, next, onUnsubscribe).toObservable

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
  def createSingleState[S, T](generator: () => S)(next: (S, Long, Observer[Observable[_ <: T]]) => Unit, onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    new AsyncOnSubscribeImpl[S, T](generator, (state, req, obs) => {next(state,req,obs); state}, onUnsubscribe).toObservable

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
  def createStateless[T](next: (Long, Observer[Observable[_ <: T]]) => Unit, onUnsubscribe: () => Unit = () => ()): Observable[T] =
    new AsyncOnSubscribeImpl[Unit, T](() => (), (_, req, obs) => next(req, obs), _ => onUnsubscribe()).toObservable

  /**
    * Generates an `Observable` that calls the provided `next` function to generate observables with at most the requested amount of items.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces an observable with at most the requested amount of items for the stream
    * @param onUnsubscribe clean up behavior
    * @return An observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def apply[S, T](generator: () => S)(next: (S, Long) => (Notification[Observable[_ <: T]], S), onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] = {
    val nextF: (S, Long, Observer[Observable[_ <: T]]) => S =
      (state, requested, observer) => next(state, requested) match {
        case (notification, nextState) =>
          notification.accept(observer)
          nextState
      }
    new AsyncOnSubscribeImpl[S, T](generator, nextF, onUnsubscribe).toObservable
  }

  /**
    * Generates an `Observable` that calls the provided `next` function to generate observables with at most the requested amount of items.
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the state value
    * @param next produces an observable with at most the requested amount of items for the stream
    * @param onUnsubscribe clean up behavior
    * @return An observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def applySingleState[S, T](generator: () => S)(next: (S, Long) => Notification[Observable[_ <: T]], onUnsubscribe: S => Unit = (_:S) => ()): Observable[T] =
    apply[S, T](generator)((s,r) => (next(s,r), s), onUnsubscribe)

  /**
    * Generates an `Observable` that calls the provided `next` function to generate observables with at most the requested amount of items.
    *
    * @tparam T the type of the generated values
    * @param next produces an observable with at most the requested amount of items for the stream
    * @param onUnsubscribe clean up behavior
    * @return An observable that emits data downstream in a protocol compatible with back-pressure.
    */
  @Experimental
  def applyStateless[T](next: Long => Notification[Observable[_ <: T]], onUnsubscribe: () => Unit = () => ()) =
    apply[Unit, T](() => ())((_,r) => (next(r), ()), _ => onUnsubscribe())

  private[scala] class AsyncOnSubscribeImpl[S,T](val generatorF: () => S, val nextF: (S, Long, Observer[Observable[_ <: T]]) => S, val onUnsubscribeF: S => Unit) {
    import rx.lang.scala.JavaConversions._

    private[scala] val asJavaAsyncOnSubscribe = new rx.observables.AsyncOnSubscribe[S,T] {
      override def generateState(): S = generatorF()
      override def next(state: S, requested: Long, observer: rx.Observer[rx.Observable[_ <: T]]): S =
        nextF(state, requested, Observer(
          onNext = t => observer.onNext(t.asJavaObservable),
          onError = e => observer.onError(e),
          onCompleted = () => observer.onCompleted()
        ))
      override def onUnsubscribe(state: S): Unit = onUnsubscribeF(state)
    }

    def toObservable = toScalaObservable(rx.Observable.create(asJavaAsyncOnSubscribe))
  }
}
