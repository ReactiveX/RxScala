package rx.lang.scala.observables

import rx.annotations.Experimental
import rx.lang.scala.{Notification, Observable}

/**
  * An utility class to create `Observable`s that start acting when subscribed to and responds
  * correctly to back pressure requests from subscribers.
  *
  * Semantics:
  * * `generator` is called to provide an initial state on each new subscription
  * * `next` is called with the last state to provide a data item and a new state for the next `next` call
  * * `onUnsubscribe` is called with the state provided by the last `next` call when the observer unsubscribes
  */
object SyncOnSubscribe {

  /**
    * Alias for [[SyncOnSubscribe.stateful]]
    * @see [[SyncOnSubscribe.stateful]]
    */
  @Experimental
  def apply[S, T](generator: () => S)(next: S => (Notification[T], S), onUnsubscribe: S => Unit = (_:S) => ()): SyncOnSubscribe[S,T] =
    stateful[S, T](generator)(next, onUnsubscribe)

  /**
    * Generates a stateful [[SyncOnSubscribe]]
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the initial state value
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def stateful[S, T](generator: () => S)(next: S => (Notification[T], S), onUnsubscribe: S => Unit = (_:S) => ()): SyncOnSubscribe[S,T] = {
    // The anonymous class shadows these names
    val nextF = next
    val onUnsubscribeF = onUnsubscribe

    new rx.observables.SyncOnSubscribe[S,T] {
      import rx.lang.scala.JavaConversions._
      override def generateState(): S = generator()
      override def next(state: S, observer: rx.Observer[_ >: T]): S =
        nextF(state) match {
          case (notification, nextState) =>
            toJavaNotification(notification).accept(observer)
            nextState
        }
      override def onUnsubscribe(state: S): Unit = onUnsubscribeF(state)
    }
  }

  /**
    * Generates a [[SyncOnSubscribe]] which does not generate a new state in `next`
    *
    * @tparam T the type of the generated values
    * @tparam S the type of the associated state with each Subscriber
    * @param generator generates the state value
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def singleState[S,T](generator: () => S)(next: S => Notification[T], onUnsubscribe: S => Unit = (_:S) => ()): SyncOnSubscribe[S,T] =
    apply[S, T](generator)(s => (next(s),s), onUnsubscribe)

  /**
    * Generates a stateless [[SyncOnSubscribe]], useful when the state is closed over in `next` or the `SyncOnSubscribe` inherently does not have a state
    *
    * @tparam T the type of the generated values
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def stateless[T](next: () => Notification[T], onUnsubscribe: () => Unit = () => ()) =
    apply[Unit, T](() => ())(_ => (next(), ()), _ => onUnsubscribe())

}
