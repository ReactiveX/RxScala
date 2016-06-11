package rx.lang.scala.observables

import rx.annotations.Experimental
import rx.lang.scala.JavaConversions._
import rx.lang.scala._
import ImplicitFunctionConversions._

/**
 * $experimental An [[Observable]] that provides operators which delay errors when composing multiple [[Observable]]s.
 *
 * @define noDefaultScheduler
 * ===Scheduler:===
 * This method does not operate by default on a particular [[Scheduler]].
 *
 * @define supportBackpressure
 * ===Backpressure:===
 * Fully supports backpressure.
 *
 * @define experimental
 * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
 */
@Experimental
class ErrorDelayingObservable[+T] private[scala](val o: Observable[T]) extends AnyVal {

  /**
   * $experimental Flattens an [[Observable]] that emits [[Observable]]s into one [[Observable]], in a way that allows an [[Observer]] to
   * receive all successfully emitted items from all of the source [[Observable]]s without being interrupted by
   * an error notification from one of them, while limiting the
   * number of concurrent subscriptions to these [[Observable]]s.
   *
   * This behaves like `flatten` except that if any of the merged [[Observable]]s notify of an
   * error via `onError`, it will refrain from propagating that
   * error notification until all of the merged [[Observable]]s have finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
   *
   * Even if multiple merged [[Observable]]s send `onError` notifications, it will only
   * invoke the `onError` method of its `Observer`s once.
   *
   * $noDefaultScheduler
   *
   * @return an [[Observable]] that emits all of the items emitted by the [[Observable]]s emitted by `this`
   * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
   */
  @Experimental
  def flatten[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = o
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.mergeDelayError[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * $experimental Flattens an [[Observable]] that emits [[Observable]]s into one [[Observable]], in a way that allows an [[Observer]] to
   * receive all successfully emitted items from all of the source [[Observable]]s without being interrupted by
   * an error notification from one of them, while limiting the
   * number of concurrent subscriptions to these [[Observable]]s.
   *
   * This behaves like `flatten` except that if any of the merged [[Observable]]s notify of an
   * error via `onError`, it will refrain from propagating that
   * error notification until all of the merged [[Observable]]s have finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
   *
   * Even if multiple merged [[Observable]]s send `onError` notifications, it will only
   * invoke the `onError` method of its `Observer`s once.
   *
   * $noDefaultScheduler
   *
   * @param maxConcurrent the maximum number of [[Observable]]s that may be subscribed to concurrently
   * @return an [[Observable]] that emits all of the items emitted by the [[Observable]]s emitted by `this`
   * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def flatten[U](maxConcurrent: Int)(implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = o
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.mergeDelayError[U](o4, maxConcurrent)
    toScalaObservable[U](o5)
  }

  /**
   * This behaves like [[rx.lang.scala.Observable.merge]] except that if any of the merged Observables
   * notify of an error via [[rx.lang.scala.Observer.onError onError]], it will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="" />
   *
   * Even if multiple merged Observables send `onError` notifications, it will only invoke the `onError` method of its
   * Observers once.
   *
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   *
   * @param that
   *            an Observable to be merged
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         `this` and `that`
   */
  def merge[U >: T](that: Observable[U]): Observable[U] = {
    toScalaObservable[U](rx.Observable.mergeDelayError[U](o.asJavaObservable, that.asJavaObservable))
  }

  /**
   * $experimental Converts this [[Observable]] that emits [[Observable]]s into an [[Observable]] that emits the items emitted by the
   * most recently emitted of those [[Observable]]s and delays any exception until all [[Observable]]s terminate.
   *
   * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.png" alt="" />
   *
   * It subscribes to an [[Observable]] that emits [[Observable]]s. Each time it observes one of
   * these emitted [[Observable]]s, the [[Observable]] returned by this method begins emitting the items
   * emitted by that [[Observable]]. When a new [[Observable]] is emitted, it stops emitting items
   * from the earlier-emitted [[Observable]] and begins emitting items from the new one.
   *
   * $noDefaultScheduler
   *
   * @return an [[Observable]] that emits the items emitted by the [[Observable]] most recently emitted by the source
   *         [[Observable]]
   * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
   */
  @Experimental
  def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = o
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.switchOnNextDelayError[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * $experimental Returns a new [[Observable]] by applying a function that you supply to each item emitted by the source
   * [[Observable]] that returns an [[Observable]], and then emitting the items emitted by the most recently emitted
   * of these [[Observable]]s and delays any error until all [[Observable]]s terminate.
   *
   * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="" />
   *
   * $noDefaultScheduler
   *
   * @param f a function that, when applied to an item emitted by the source [[Observable]], returns an [[Observable]]
   * @return an [[Observable]] that emits the items emitted by the [[Observable]] returned from applying `f` to the most
   *         recently emitted item emitted by the source [[Observable]]
   * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
   */
  @Experimental
  def switchMap[R](f: T => Observable[R]): Observable[R] = {
    val jf: rx.functions.Func1[T, rx.Observable[_ <: R]] = (t: T) => f(t).asJavaObservable
    toScalaObservable[R](o.asJavaObservable.switchMapDelayError[R](jf))
  }

  /**
   * $experimental Concatenates the [[Observable]] sequence of [[Observable]]s into a single sequence by subscribing to
   * each inner [[Observable]], one after the other, one at a time and delays any errors till the all inner and the
   * outer [[Observable]]s terminate.
   *
   * $supportBackpressure
   *
   * $noDefaultScheduler
   *
   * @return the new [[Observable]] with the concatenating behavior
   */
  @Experimental
  def concat[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = o
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.concatDelayError[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * $experimental Maps each of the items into an [[Observable]], subscribes to them one after the other,
   * one at a time and emits their values in order while delaying any error from either this or any of the inner [[Observable]]s
   * till all of them terminate.
   *
   * $supportBackpressure
   *
   * $noDefaultScheduler
   *
   * @param f the function that maps the items of this [[Observable]] into the inner [[Observable]]s.
   * @return the new [[Observable]] instance with the concatenation behavior
   */
  @Experimental
  def concatMap[R](f: T => Observable[R]): Observable[R] = {
    val jf: rx.functions.Func1[T, rx.Observable[_ <: R]] = (t: T) => f(t).asJavaObservable
    toScalaObservable[R](o.asJavaObservable.concatMapDelayError[R](jf))
  }
}
