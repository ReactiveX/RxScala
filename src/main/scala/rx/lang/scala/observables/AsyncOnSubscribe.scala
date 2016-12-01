package rx.lang.scala.observables

import rx.annotations.Experimental
import rx.lang.scala.{Notification, Observable}

/**
  * An utility class to create `Observable`s that start acting when subscribed to and responds
  * correctly to back pressure requests from subscribers.
  *
  * Semantics:
  * * `generator` is called to provide an initial state on each new subscription
  * * `next` is called with the last state and a `requested` amount of items to provide a new state
  *     and an `Observable` that (potentially asynchronously) emits up to `requested` items.
  * * `onUnsubscribe` is called with the state provides by the last next when the observer unsubscribes
  */
object AsyncOnSubscribe {

  /**
    * Alias for [[AsyncOnSubscribe.stateful]]
    * @see [[AsyncOnSubscribe.stateful]]
    */
  @Experimental
  def apply[S,T](generator: () => S)(next: (S, Long) => (Notification[Observable[T]], S), onUnsubscribe: S => Unit = (_:S) => ()): AsyncOnSubscribe[S,T] =
    stateful[S, T](generator)(next, onUnsubscribe)

  /**
    * Generates a stateful [[AsyncOnSubscribe]]
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces observables which contain data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def stateful[S, T](generator: () => S)(next: (S, Long) => (Notification[Observable[T]], S), onUnsubscribe: S => Unit = (_:S) => ()): AsyncOnSubscribe[S,T] = {
    // The anonymous class shadows these names
    val nextF = next
    val onUnsubscribeF = onUnsubscribe

    new rx.observables.AsyncOnSubscribe[S,T] {
      import rx.lang.scala.JavaConversions._
      override def generateState(): S = generator()
      override def next(state: S, requested: Long, observer: rx.Observer[rx.Observable[_ <: T]]): S =
        nextF(state, requested) match {
          case (notification, nextState) =>
            toJavaNotification(notification.map(toJavaObservable)).accept(observer)
            nextState
        }
      override def onUnsubscribe(state: S): Unit = onUnsubscribeF(state)
    }
  }

  /**
    * Generates a [[AsyncOnSubscribe]] which does not generate a new state in `next`
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the state value
    * @param next produces observables which contain data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def singleState[S, T](generator: () => S)(next: (S, Long) => Notification[Observable[T]], onUnsubscribe: S => Unit = (_:S) => ()): AsyncOnSubscribe[S,T] =
    stateful[S, T](generator)((s,r) => (next(s,r), s), onUnsubscribe)

  /**
    * Generates a stateless [[AsyncOnSubscribe]], useful when the state is closed over in `next` or the `SyncOnSubscribe` inherently does not have a state
    *
    * @tparam T the type of the generated values
    * @param next produces observables which contain data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def stateless[T](next: Long => Notification[Observable[T]], onUnsubscribe: () => Unit = () => ()) =
    stateful[Unit, T](() => ())((_,r) => (next(r), ()), _ => onUnsubscribe())

}
