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
  * * `onUnsubscribe` is called with the state provides by the last next when the observer unsubscribes
  *
  * @tparam S the type of the user-define state used
  * @tparam T the type items that this `SyncOnSubscribe` will emit.
  */
class SyncOnSubscribe[S,T](val generator: () => S,
                            val next: S => (Notification[T], S),
                            val onUnsubscribe: S => Unit) { self =>
  import rx.lang.scala.JavaConversions._

  private[scala] val asJavaSyncOnSubscribe = new rx.observables.SyncOnSubscribe[S,T] {
    override def generateState(): S = generator()
    override def next(state: S, observer: rx.Observer[_ >: T]): S =
      self.next(state) match {
        case (notification, nextState) =>
          toJavaNotification(notification).accept(observer)
          nextState
      }
    override def onUnsubscribe(state: S): Unit = self.onUnsubscribe(state)
  }

  def toObservable: Observable[T] = toScalaObservable[T](rx.Observable.create(asJavaSyncOnSubscribe))
}

object SyncOnSubscribe {

  /**
    * Alias for [[SyncOnSubscribe.stateful]]
    * @see [[SyncOnSubscribe.stateful]]
    */
  @Experimental
  def apply[S, T](generator: () => S)(next: S => (Notification[T], S), onUnsubscribe: S => Unit = (_:S) => ()): SyncOnSubscribe[S,T] =
    stateful(generator)(next, onUnsubscribe)

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
  def stateful[S, T](generator: () => S)(next: S => (Notification[T], S), onUnsubscribe: S => Unit = (_:S) => ()): SyncOnSubscribe[S,T] =
    new SyncOnSubscribe(generator, next, onUnsubscribe)

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
    apply(generator)(s => (next(s),s), onUnsubscribe)

  /**
    * Generates a stateless [[SyncOnSubscribe]], useful when the state is closed over in `next` or the `SyncOnSubscribe` inherently does not have a state
    *
    * @tparam T the type of the generated values
    * @param next produces data for the stream
    * @param onUnsubscribe clean up behavior
    */
  @Experimental
  def stateless[T](next: () => Notification[T], onUnsubscribe: () => Unit = () => ()) =
    apply(() => ())(_ => (next(), ()), _ => onUnsubscribe())

}
