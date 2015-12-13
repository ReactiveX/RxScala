/**
 * Copyright 2013 Netflix, Inc.
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

import rx.annotations.{Beta, Experimental}
import rx.exceptions.OnErrorNotImplementedException
import rx.functions.FuncN
import rx.lang.scala.observables.ConnectableObservable
import scala.concurrent.duration
import java.util
import collection.JavaConversions._
import scala.collection.generic.CanBuildFrom
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{Iterable, Traversable, immutable}
import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds
import scala.reflect.ClassTag


/**
 * The Observable interface that implements the Reactive Pattern.
 *
 * @define subscribeObserverMain
 * Call this method to subscribe an [[rx.lang.scala.Observer]] for receiving 
 * items and notifications from the Observable.
 *
 * A typical implementation of `subscribe` does the following:
 *
 * It stores a reference to the Observer in a collection object, such as a `List[T]` object.
 *
 * It returns a reference to the [[rx.lang.scala.Subscription]] interface. This enables Observers to
 * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
 * sending them, which also invokes the Observer's [[rx.lang.scala.Observer.onCompleted onCompleted]] method.
 *
 * An `Observable[T]` instance is responsible for accepting all subscriptions
 * and notifying all Observers. Unless the documentation for a particular
 * `Observable[T]` implementation indicates otherwise, Observers should make no
 * assumptions about the order in which multiple Observers will receive their notifications.
 *
 * @define subscribeObserverParamObserver 
 *         the observer
 * @define subscribeObserverParamScheduler 
 *         the [[rx.lang.scala.Scheduler]] on which Observers subscribe to the Observable
 *
 * @define subscribeSubscriberMain
 * Call this method to subscribe an [[Subscriber]] for receiving items and notifications from the [[Observable]].
 *
 * A typical implementation of `subscribe` does the following:
 *
 * It stores a reference to the Observer in a collection object, such as a `List[T]` object.
 *
 * It returns a reference to the [[rx.lang.scala.Subscription]] interface. This enables [[Subscriber]]s to
 * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
 * sending them, which also invokes the Subscriber's [[rx.lang.scala.Observer.onCompleted onCompleted]] method.
 *
 * An [[Observable]] instance is responsible for accepting all subscriptions
 * and notifying all [[Subscriber]]s. Unless the documentation for a particular
 * [[Observable]] implementation indicates otherwise, [[Subscriber]]s should make no
 * assumptions about the order in which multiple [[Subscriber]]s will receive their notifications.
 *
 * @define subscribeSubscriberParamObserver
 *         the [[Subscriber]]
 * @define subscribeSubscriberParamScheduler
 *         the [[rx.lang.scala.Scheduler]] on which [[Subscriber]]s subscribe to the Observable
 *
 * @define subscribeAllReturn 
 *         a [[rx.lang.scala.Subscription]] reference whose `unsubscribe` method can be called to  stop receiving items
 *         before the Observable has finished sending them
 *
 * @define subscribeCallbacksMainWithNotifications
 * Call this method to receive items and notifications from this observable.
 *
 * @define subscribeCallbacksMainNoNotifications
 * Call this method to receive items from this observable.
 *
 * @define subscribeCallbacksParamOnNext 
 *         this function will be called whenever the Observable emits an item
 * @define subscribeCallbacksParamOnError
 *         this function will be called if an error occurs
 * @define subscribeCallbacksParamOnComplete
 *         this function will be called when this Observable has finished emitting items
 * @define subscribeCallbacksParamScheduler
 *         the scheduler to use
 *
 * @define noDefaultScheduler
 * ===Scheduler:===
 * This method does not operate by default on a particular [[Scheduler]].
 *
 * @define debounceVsThrottle
 * Information on debounce vs throttle:
 *  - [[http://drupalmotion.com/article/debounce-and-throttle-visual-explanation]]
 *  - [[http://unscriptable.com/2009/03/20/debouncing-javascript-methods/]]
 *  - [[http://www.illyriad.co.uk/blog/index.php/2011/09/javascript-dont-spam-your-server-debounce-and-throttle/]]
 *
 * @define experimental
 * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
 *
 * @define beta
 * <span class="badge badge-red" style="float: right;">BETA</span>
 *
 */
trait Observable[+T]
{
  import scala.collection.JavaConverters._
  import scala.collection.Seq
  import scala.concurrent.duration.{Duration, TimeUnit, MILLISECONDS}
  import scala.collection.mutable
  import rx.functions._
  import rx.lang.scala.observables.BlockingObservable
  import ImplicitFunctionConversions._
  import JavaConversions._

  private [scala] val asJavaObservable: rx.Observable[_ <: T]

  /**
   * Subscribes to an [[Observable]] but ignore its emissions and notifications.
   *
   * $noDefaultScheduler
   *
   * @return $subscribeAllReturn
   * @throws rx.exceptions.OnErrorNotImplementedException if the [[Observable]] tries to call `onError`
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(): Subscription = {
    asJavaObservable.subscribe()
  }

  /**
   * $subscribeObserverMain
   *
   * $noDefaultScheduler
   *
   * @param observer $subscribeObserverParamObserver
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(observer: Observer[T]): Subscription = {
    asJavaObservable.subscribe(observer.asJavaObserver)
  }

  /**
   * $subscribeObserverMain
   *
   * $noDefaultScheduler
   *
   * @param observer $subscribeObserverParamObserver
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def apply(observer: Observer[T]): Subscription = subscribe(observer)

  /**
   * $subscribeSubscriberMain
   *
   * $noDefaultScheduler
   *
   * @param subscriber $subscribeSubscriberParamObserver
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(subscriber: Subscriber[T]): Subscription = {
    // Add the casting to avoid compile error "ambiguous reference to overloaded definition"
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    thisJava.subscribe(subscriber.asJavaSubscriber)
  }

  /**
   * Subscribe to Observable and invoke `OnSubscribe` function without any
   * contract protection, error handling, unsubscribe, or execution hooks.
   *
   * This should only be used for implementing an `Operator` that requires nested subscriptions.
   *
   * Normal use should use `Observable.subscribe` which ensures the Rx contract and other functionality.
   *
   * @param subscriber
   * @return [[Subscription]] which is the Subscriber passed in
   * @since 0.17
   */
  def unsafeSubscribe(subscriber: Subscriber[T]): Subscription = {
    asJavaObservable.unsafeSubscribe(subscriber.asJavaSubscriber)
  }

  /**
   * $subscribeSubscriberMain
   *
   * $noDefaultScheduler
   *
   * @param subscriber $subscribeSubscriberParamObserver
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def apply(subscriber: Subscriber[T]): Subscription = subscribe(subscriber)

  /**
   * $subscribeCallbacksMainNoNotifications
   *
   * $noDefaultScheduler
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @return $subscribeAllReturn
   * @throws rx.exceptions.OnErrorNotImplementedException if the [[Observable]] tries to call `onError`
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(onNext: T => Unit): Subscription = {
   asJavaObservable.subscribe(scalaFunction1ProducingUnitToAction1(onNext))
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * $noDefaultScheduler
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit): Subscription = {
    asJavaObservable.subscribe(
      scalaFunction1ProducingUnitToAction1(onNext),
      scalaFunction1ProducingUnitToAction1(onError)
    )
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * $noDefaultScheduler
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @param onCompleted $subscribeCallbacksParamOnComplete
   * @return $subscribeAllReturn
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Subscription = {
    asJavaObservable.subscribe(
      scalaFunction1ProducingUnitToAction1(onNext),
      scalaFunction1ProducingUnitToAction1(onError), 
      scalaFunction0ProducingUnitToAction0(onCompleted)
    )
  }

  /**
   * Returns an Observable that first emits the items emitted by `this`, and then `elem`.
   *
   * @param elem the item to be appended
   * @return  an Observable that first emits the items emitted by `this`, and then `elem`.
   */
  def :+[U >: T](elem: U): Observable[U] = {
    this ++ Observable.just(elem)
  }

  /**
   * Returns an Observable that first emits the items emitted by `this`, and then the items emitted
   * by `that`.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="" />
   *
   * @param that
   *            an Observable to be appended
   * @return an Observable that emits items that are the result of combining the items emitted by
   *         this and that, one after the other
   */
  def ++[U >: T](that: Observable[U]): Observable[U] = {
    val o1: rx.Observable[_ <: U] = this.asJavaObservable
    val o2: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable(rx.Observable.concat(o1, o2))
  }

  /**
   * Returns an Observable that emits a specified item before it begins to emit items emitted by the source Observable.
   * <p>
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.png" alt="" />
   *
   * @param elem the item to emit
   * @return an Observable that emits the specified item before it begins to emit items emitted by the source Observable
   */
  def +:[U >: T](elem: U): Observable[U] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable(thisJava.startWith(elem))
  }

  /**
   * Returns an Observable that emits the items emitted by several Observables, one after the
   * other.
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @usecase def concat[U]: Observable[U]
   */
  def concat[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.concat[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * Returns a new Observable that emits items resulting from applying a function that you supply to each item
   * emitted by the source Observable, where that function returns an Observable, and then emitting the items
   * that result from concatinating those resulting Observables.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="" />
   *
   * @param f a function that, when applied to an item emitted by the source Observable, returns an Observable
   * @return an Observable that emits the result of applying the transformation function to each item emitted
   *         by the source Observable and concatinating the Observables obtained from this transformation
   */
  def concatMap[R](f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.concatMap[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t1: T): rx.Observable[_ <: R] = {
        f(t1).asJavaObservable
      }
    }))
  }

  /**
   * $experimental Concatenates `this` and `that` source [[Observable]]s eagerly into a single stream of values.
   *
   * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
   * source [[Observable]]s. The operator buffers the values emitted by these [[Observable]]s and then drains them
   * in order, each one after the previous one completes.
   *
   * ===Backpressure:===
   * Backpressure is honored towards the downstream, however, due to the eagerness requirement, sources
   * are subscribed to in unbounded mode and their values are queued up in an unbounded buffer.
   *
   * $noDefaultScheduler
   *
   * @param that the source to concat with.
   * @return an [[Observable]] that emits items all of the items emitted by `this` and `that`, one after the other,
   *         without interleaving them.
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def concatEager[U >: T](that: Observable[U]): Observable[U] = {
    val o1: rx.Observable[_ <: U] = this.asJavaObservable
    val o2: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable(rx.Observable.concatEager(o1, o2))
  }

  /**
   * $experimental Concatenates an [[Observable]] sequence of [[Observable]]s eagerly into a single stream of values.
   *
   * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
   * emitted source [[Observable]]s as they are observed. The operator buffers the values emitted by these
   * [[Observable]]s and then drains them in order, each one after the previous one completes.
   *
   * ===Backpressure:===
   * Backpressure is honored towards the downstream, however, due to the eagerness requirement, sources
   * are subscribed to in unbounded mode and their values are queued up in an unbounded buffer.
   *
   * $noDefaultScheduler
   *
   * @return an [[Observable]] that emits items all of the items emitted by the [[Observable]]s emitted by
   *         `this`, one after the other, without interleaving them
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def concatEager[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.concatEager[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * $experimental Concatenates an [[Observable]] sequence of [[Observable]]s eagerly into a single stream of values.
   *
   * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
   * emitted source [[Observable]]s as they are observed. The operator buffers the values emitted by these
   * [[Observable]]s and then drains them in order, each one after the previous one completes.
   *
   * ===Backpressure:===
   * Backpressure is honored towards the downstream, however, due to the eagerness requirement, sources
   * are subscribed to in unbounded mode and their values are queued up in an unbounded buffer.
   *
   * $noDefaultScheduler
   *
   * @param capacityHint hints about the number of expected values in an [[Observable]]
   * @return an [[Observable]] that emits items all of the items emitted by the [[Observable]]s emitted by
   *         `this`, one after the other, without interleaving them
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def concatEager[U](capacityHint: Int)(implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.concatEager[U](o4, capacityHint)
    toScalaObservable[U](o5)
  }

  /**
   * $experimental Maps a sequence of values into [[Observable]]s and concatenates these [[Observable]]s eagerly into a single
   * Observable.
   *
   * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
   * source [[Observable]]s. The operator buffers the values emitted by these [[Observable]]s and then drains them in
   * order, each one after the previous one completes.
   *
   * ===Backpressure:===
   * Backpressure is honored towards the downstream, however, due to the eagerness requirement, sources
   * are subscribed to in unbounded mode and their values are queued up in an unbounded buffer.
   *
   * $noDefaultScheduler
   *
   * @param f the function that maps a sequence of values into a sequence of [[Observable]]s that will be
   *          eagerly concatenated
   * @return an [[Observable]] that emits items all of the items emitted by the [[Observable]]s returned by
   *         `f`, one after the other, without interleaving them
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def concatMapEager[R](f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.concatMapEager[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t1: T): rx.Observable[_ <: R] = {
        f(t1).asJavaObservable
      }
    }))
  }

  /**
   * $experimental Maps a sequence of values into [[Observable]]s and concatenates these [[Observable]]s eagerly into a single
   * Observable.
   *
   * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
   * source [[Observable]]s. The operator buffers the values emitted by these [[Observable]]s and then drains them in
   * order, each one after the previous one completes.
   *
   * ===Backpressure:===
   * Backpressure is honored towards the downstream, however, due to the eagerness requirement, sources
   * are subscribed to in unbounded mode and their values are queued up in an unbounded buffer.
   *
   * $noDefaultScheduler
   *
   * @param f the function that maps a sequence of values into a sequence of [[Observable]]s that will be
   *          eagerly concatenated
   * @param capacityHint hints about the number of expected values in an [[Observable]]
   * @return an [[Observable]] that emits items all of the items emitted by the [[Observable]]s returned by
   *         `f`, one after the other, without interleaving them
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def concatMapEager[R](f: T => Observable[R], capacityHint: Int): Observable[R] = {
    toScalaObservable[R](asJavaObservable.concatMapEager[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t1: T): rx.Observable[_ <: R] = {
        f(t1).asJavaObservable
      }
    }, capacityHint))
  }

  /**
   * Wraps this Observable in another Observable that ensures that the resulting
   * Observable is chronologically well-behaved.
   *
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/synchronize.png" alt="" />
   *
   * A well-behaved Observable does not interleave its invocations of the [[rx.lang.scala.Observer.onNext onNext]], [[rx.lang.scala.Observer.onCompleted onCompleted]], and [[rx.lang.scala.Observer.onError onError]] methods of
   * its [[rx.lang.scala.Observer]]s; it invokes `onCompleted` or `onError` only once; and it never invokes `onNext` after invoking either `onCompleted` or `onError`.
   * [[Observable.serialize serialize]] enforces this, and the Observable it returns invokes `onNext` and `onCompleted` or `onError` synchronously.
   *
   * @return an Observable that is a chronologically well-behaved version of the source
   *         Observable, and that synchronously notifies its [[rx.lang.scala.Observer]]s
   */
  def serialize: Observable[T] = {
    toScalaObservable[T](asJavaObservable.serialize)
  }

  /**
   * Wraps each item emitted by a source Observable in a timestamped tuple.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.png" alt="" />
   *
   * @return an Observable that emits timestamped items from the source Observable
   */
  def timestamp: Observable[(Long, T)] = {
    toScalaObservable[rx.schedulers.Timestamped[_ <: T]](asJavaObservable.timestamp())
      .map((t: rx.schedulers.Timestamped[_ <: T]) => (t.getTimestampMillis, t.getValue))
  }

  /**
   * Wraps each item emitted by a source Observable in a timestamped tuple
   * with timestamps provided by the given Scheduler.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.s.png" alt="" />
   * 
   * @param scheduler [[rx.lang.scala.Scheduler]] to use as a time source.
   * @return an Observable that emits timestamped items from the source
   *         Observable with timestamps provided by the given Scheduler
   */
  def timestamp(scheduler: Scheduler): Observable[(Long, T)] = {
    toScalaObservable[rx.schedulers.Timestamped[_ <: T]](asJavaObservable.timestamp(scheduler))
      .map((t: rx.schedulers.Timestamped[_ <: T]) => (t.getTimestampMillis, t.getValue))
  }

  /**
   * Returns an Observable formed from this Observable and another Observable by combining 
   * corresponding elements in pairs. 
   * The number of `onNext` invocations of the resulting `Observable[(T, U)]`
   * is the minumum of the number of `onNext` invocations of `this` and `that`.
   *
   * @param that the Observable to zip with
   * @return an Observable that pairs up values from `this` and `that` Observables.
   */
  def zip[U](that: Observable[U]): Observable[(T, U)] = {
    zipWith(that)((_, _))
  }

  /**
   * Returns an Observable formed from `this` Observable and `other` Iterable by combining
   * corresponding elements in pairs.
   * <p>
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.i.png" alt="" />
   * <p>
   * Note that the `other` Iterable is evaluated as items are observed from the source Observable; it is
   * not pre-consumed. This allows you to zip infinite streams on either side.
   *
   * @param that the Iterable sequence
   * @return an Observable that pairs up values from the source Observable and the `other` Iterable.
   */
  def zip[U](that: Iterable[U]): Observable[(T, U)] = {
    zipWith(that)((_, _))
  }

  /**
   * Returns an Observable that emits items that are the result of applying a specified function to pairs of
   * values, one each from the source Observable and a specified Iterable sequence.
   * <p>
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.i.png" alt="" />
   * <p>
   * Note that the `other` Iterable is evaluated as items are observed from the source Observable; it is
   * not pre-consumed. This allows you to zip infinite streams on either side.
   *
   * @param that the Iterable sequence
   * @param selector a function that combines the pairs of items from the Observable and the Iterable to generate
   *                 the items to be emitted by the resulting Observable
   * @return an Observable that pairs up values from the source Observable and the `other` Iterable
   *         sequence and emits the results of `selector` applied to these pairs
   */
  def zipWith[U, R](that: Iterable[U])(selector: (T, U) => R): Observable[R] = {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    toScalaObservable[R](thisJava.zipWith(that.asJava, selector))
  }

  /**
   * Returns an Observable formed from this Observable and another Observable by combining
   * corresponding elements using the selector function.
   * The number of `onNext` invocations of the resulting `Observable[(T, U)]`
   * is the minumum of the number of `onNext` invocations of `this` and `that`.
   *
   * @param that the Observable to zip with
   * @return an Observable that pairs up values from `this` and `that` Observables.
   */
  def zipWith[U, R](that: Observable[U])(selector: (T, U) => R): Observable[R] = {
    toScalaObservable[R](rx.Observable.zip[T, U, R](this.asJavaObservable, that.asJavaObservable, selector))
  }

  /**
   * Zips this Observable with its indices.
   *
   * @return An Observable emitting pairs consisting of all elements of this Observable paired with 
   *         their index. Indices start at 0.
   */
  def zipWithIndex: Observable[(T, Int)] = {
    zip(0 until Int.MaxValue)
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces buffers. Buffers are created when the specified `openings`
   * Observable produces an object. That object is used to construct an Observable to emit buffers, feeding it into `closings` function.
   * Buffers are emitted when the created Observable produces an object.
   *
   * @param openings
   *            The [[rx.lang.scala.Observable]] which, when it produces an object, will cause
   *            another buffer to be created.
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every buffer created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated buffer
   *            is emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces buffers which are created and emitted when the specified [[rx.lang.scala.Observable]]s publish certain objects.
   */
  def slidingBuffer[Opening](openings: Observable[Opening])(closings: Opening => Observable[Any]): Observable[Seq[T]] = {
    val opening: rx.Observable[_ <: Opening] = openings.asJavaObservable
    val closing: Func1[_ >: Opening, _ <: rx.Observable[_ <: Any]] = (o: Opening) => closings(o).asJavaObservable
    val jObs: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer[Opening, Any](opening, closing)
    Observable.jObsOfListToScObsOfSeq(jObs.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each containing `count`
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers containing at most
   *         `count` produced values.
   */
  def tumblingBuffer(count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(count)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces buffers every `skip` values, each containing `count`
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new buffer. Note that when `skip` and
   *            `count` are equals that this is the same operation as `buffer(int)`.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces buffers every `skip` values containing at most
   *         `count` produced values.
   */
  def slidingBuffer(count: Int, skip: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(count, skip)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the `timespan` argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def tumblingBuffer(timespan: Duration): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the `timespan` argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def tumblingBuffer(timespan: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def tumblingBuffer(timespan: Duration, count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, count)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def tumblingBuffer(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, count, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the `timeshift` argument. Each buffer is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def slidingBuffer(timespan: Duration, timeshift: Duration): Observable[Seq[T]] = {
    val span: Long = timespan.toNanos
    val shift: Long = timespan.toNanos
    val unit: TimeUnit = duration.NANOSECONDS
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(span, shift, unit)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the `timeshift` argument. Each buffer is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def slidingBuffer(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val span: Long = timespan.toNanos
    val shift: Long = timespan.toNanos
    val unit: TimeUnit = duration.NANOSECONDS
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(span, shift, unit, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Returns an Observable that emits non-overlapping buffered items from the source Observable each time the
   * specified boundary Observable emits an item.
   * <p>
   * <img width="640" height="395" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.png" alt="" />
   * <p>
   * Completion of either the source or the boundary Observable causes the returned Observable to emit the
   * latest buffer and complete.
   *
   * @param boundary the boundary Observable. Note: This is a by-name parameter,
   *                 so it is only evaluated when someone subscribes to the returned Observable.
   * @return an Observable that emits buffered items from the source Observable when the boundary Observable
   *         emits an item
   */
  def tumblingBuffer(boundary: => Observable[Any]): Observable[Seq[T]] = {
    val f = new Func0[rx.Observable[_ <: Any]]() {
      override def call(): rx.Observable[_ <: Any] = boundary.asJavaObservable
    }
    toScalaObservable(asJavaObservable.buffer[Any](f)).map(_.asScala)
  }

  /**
   * Returns an Observable that emits non-overlapping buffered items from the source Observable each time the
   * specified boundary Observable emits an item.
   * <p>
   * <img width="640" height="395" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.png" alt="" />
   * <p>
   * Completion of either the source or the boundary Observable causes the returned Observable to emit the
   * latest buffer and complete.
   *
   * @param boundary the boundary Observable
   * @param initialCapacity the initial capacity of each buffer chunk
   * @return an Observable that emits buffered items from the source Observable when the boundary Observable
   *         emits an item
   */
  def tumblingBuffer(boundary: Observable[Any], initialCapacity: Int): Observable[Seq[T]] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    toScalaObservable(thisJava.buffer(boundary.asJavaObservable, initialCapacity)).map(_.asScala)
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows. The boundary of each window is determined by the items emitted from a specified
   * boundary-governing Observable.
   *
   * <img width="640" height="475" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/window8.png" alt="" />
   *
   * @param boundary an Observable whose emitted items close and open windows. Note: This is a by-name parameter,
   *                 so it is only evaluated when someone subscribes to the returned Observable.
   * @return An Observable which produces connected non-overlapping windows. The boundary of each window is
   *         determined by the items emitted from a specified boundary-governing Observable.
   */
  def tumbling(boundary: => Observable[Any]): Observable[Observable[T]] = {
    val func = new Func0[rx.Observable[_ <: Any]]() {
      override def call(): rx.Observable[_ <: Any] = boundary.asJavaObservable
    }
    val jo: rx.Observable[_ <: rx.Observable[_ <: T]] = asJavaObservable.window[Any](func)
    toScalaObservable(jo).map(toScalaObservable[T](_))
  }

  /**
   * Creates an Observable which produces windows of collected values. Chunks are created when the specified `openings`
   * Observable produces an object. That object is used to construct an Observable to emit windows, feeding it into `closings` function.
   * Windows are emitted when the created Observable produces an object.
   *
   * @param openings
   *            The [[rx.lang.scala.Observable]] which when it produces an object, will cause
   *            another window to be created.
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every window created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated window
   *            is emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces windows which are created and emitted when the specified [[rx.lang.scala.Observable]]s publish certain objects.
   */
  def sliding[Opening](openings: Observable[Opening])(closings: Opening => Observable[Any]) = {
    Observable.jObsOfJObsToScObsOfScObs(
      asJavaObservable.window[Opening, Any](openings.asJavaObservable, (op: Opening) => closings(op).asJavaObservable))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each containing `count` elements. When the source Observable completes or
   * encounters an error, the current window is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows containing at most
   *         `count` produced values.
   */
  def tumbling(count: Int): Observable[Observable[T]] = {
    // this unnecessary ascription is needed because of this bug (without, compiler crashes):
    // https://issues.scala-lang.org/browse/SI-7818
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(count)) : Observable[Observable[T]]
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces windows every
   * `skip` values, each containing `count` elements. When the source Observable completes or encounters an error,
   * the current window is emitted and the event is propagated.
   *
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new window. Note that when `skip` and
   *            `count` are equal that this is the same operation as `window(int)`.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces windows every `skip` values containing at most
   *         `count` produced values.
   */
  def sliding(count: Int, skip: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(count, skip))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def tumbling(timespan: Duration): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def tumbling(timespan: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def tumbling(timespan: Duration, count: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, count))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def tumbling(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, count, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the `timeshift` argument. Each window is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def sliding(timespan: Duration, timeshift: Duration): Observable[Observable[T]] = {
    val span: Long = timespan.toNanos
    val shift: Long = timespan.toNanos
    val unit: TimeUnit = duration.NANOSECONDS
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(span, shift, unit))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the `timeshift` argument. Each window is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def sliding(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    val span: Long = timespan.toNanos
    val shift: Long = timespan.toNanos
    val unit: TimeUnit = duration.NANOSECONDS
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(span, shift, unit, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Returns an Observable that emits windows of items it collects from the source Observable. The resulting
   * Observable starts a new window periodically, as determined by the `timeshift` argument or a maximum
   * size as specified by the `count` argument (whichever is reached first). It emits
   * each window after a fixed timespan, specified by the `timespan` argument. When the source
   * Observable completes or Observable completes or encounters an error, the resulting Observable emits the
   * current window and propagates the notification from the source Observable.
   *
   * <img width="640" height="335" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.s.png" alt="" />
   *
   * ===Backpressure Support:===
   * This operator does not support backpressure as it uses time to control data flow.
   *
   * ===Scheduler:===
   * you specify which `Scheduler` this operator will use
   *
   * @param timespan the period of time each window collects items before it should be emitted
   * @param timeshift the period of time after which a new window will be created
   * @param count the maximum size of each window before it should be emitted
   * @param scheduler the `Scheduler` to use when determining the end and start of a window
   * @return an Observable that emits new windows periodically as a fixed timespan elapses
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#window">RxJava wiki: window</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.window.aspx">MSDN: Observable.Window</a>
   */
  def sliding(timespan: Duration, timeshift: Duration, count: Int, scheduler: Scheduler): Observable[Observable[T]] = {
    val span: Long = timespan.toNanos
    val shift: Long = timespan.toNanos
    val unit: TimeUnit = duration.NANOSECONDS
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(span, shift, unit, count, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Returns an Observable which only emits those items for which a given predicate holds.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.png" alt="" />
   *
   * @param predicate
   *            a function that evaluates the items emitted by the source Observable, returning `true` if they pass the filter
   * @return an Observable that emits only those items in the original Observable that the filter
   *         evaluates as `true`
   */
  def filter(predicate: T => Boolean): Observable[T] = {
    toScalaObservable[T](asJavaObservable.filter(predicate))
  }

  /**
   * Registers an function to be called when this Observable invokes [[rx.lang.scala.Observer.onCompleted onCompleted]] or [[rx.lang.scala.Observer.onError onError]].
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/finallyDo.png" alt="" />
   *
   * @param action
   *            an function to be invoked when the source Observable finishes
   * @return an Observable that emits the same items as the source Observable, then invokes the function
   */
  def finallyDo(action: => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.finallyDo(() => action))
  }

  /**
   * Creates a new Observable by applying a function that you supply to each item emitted by
   * the source Observable, where that function returns an Observable, and then merging those
   * resulting Observables and emitting the results of this merger.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMap.png" alt="" />
   *
   * @param f
   *            a function that, when applied to an item emitted by the source Observable, returns
   *            an Observable
   * @return an Observable that emits the result of applying the transformation function to each
   *         item emitted by the source Observable and merging the results of the Observables
   *         obtained from this transformation.
   */
  def flatMap[R](f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]]{
      def call(t1: T): rx.Observable[_ <: R] = { f(t1).asJavaObservable }
    }))
  }

  /**
   * $beta Returns an [[Observable]] that emits items based on applying a function that you supply to each item emitted
   * by the source [[Observable]] , where that function returns an [[Observable]] , and then merging those resulting
   * [[Observable]]s and emitting the results of this merger, while limiting the maximum number of concurrent
   * subscriptions to these [[Observable]]s.
   *
   * $$noDefaultScheduler
   *
   * @param maxConcurrent the maximum number of [[Observable]]s that may be subscribed to concurrently
   * @param f a function that, when applied to an item emitted by the source [[Observable]], returns an [[Observable]]
   * @return an [[Observable]] that emits the result of applying the transformation function to each item emitted
   *         by the source [[Observable]] and merging the results of the [[Observable]]s obtained from this transformation
   * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Beta
  def flatMap[R](maxConcurrent: Int, f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t1: T): rx.Observable[_ <: R] = {
        f(t1).asJavaObservable
      }
    }, maxConcurrent))
  }

  /**
   * Returns an Observable that applies a function to each item emitted or notification raised by the source
   * Observable and then flattens the Observables returned from these functions and emits the resulting items.
   *
   * <img width="640" height="410" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.png" alt="" />
   *
   * @tparam R the result type
   * @param onNext a function that returns an Observable to merge for each item emitted by the source Observable
   * @param onError a function that returns an Observable to merge for an onError notification from the source
   *                Observable
   * @param onCompleted a function that returns an Observable to merge for an onCompleted notification from the source
   *                    Observable
   * @return an Observable that emits the results of merging the Observables returned from applying the
   *         specified functions to the emissions and notifications of the source Observable
   */
  def flatMap[R](onNext: T => Observable[R], onError: Throwable => Observable[R], onCompleted: () => Observable[R]): Observable[R] = {
    val jOnNext = new Func1[T, rx.Observable[_ <: R]] {
      override def call(t: T): rx.Observable[_ <: R] = onNext(t).asJavaObservable
    }
    val jOnError = new Func1[Throwable, rx.Observable[_ <: R]] {
      override def call(e: Throwable): rx.Observable[_ <: R] = onError(e).asJavaObservable
    }
    val jOnCompleted = new Func0[rx.Observable[_ <: R]] {
      override def call(): rx.Observable[_ <: R] = onCompleted().asJavaObservable
    }
    toScalaObservable[R](asJavaObservable.flatMap[R](jOnNext, jOnError, jOnCompleted))
  }

  /**
   * $beta Returns an [[Observable]] that applies a function to each item emitted or notification raised by the source
   * [[Observable]]  and then flattens the [[Observable]] s returned from these functions and emits the resulting items,
   * while limiting the maximum number of concurrent subscriptions to these [[Observable]]s.
   *
   * $noDefaultScheduler
   *
   * @param maxConcurrent the maximum number of [[Observable]]s that may be subscribed to concurrently
   * @param onNext a function that returns an [[Observable]] to merge for each item emitted by the source [[Observable]]
   * @param onError a function that returns an [[Observable]] to merge for an onError notification from the source [[Observable]]
   * @param onCompleted a function that returns an [[Observable]] to merge for an onCompleted notification from the source [[Observable]]
   * @return an [[Observable]] that emits the results of merging the [[Observable]]s returned from applying the
   *         specified functions to the emissions and notifications of the source [[Observable]]
   * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Beta
  def flatMap[R](maxConcurrent: Int, onNext: T => Observable[R], onError: Throwable => Observable[R], onCompleted: () => Observable[R]): Observable[R] = {
    val jOnNext = new Func1[T, rx.Observable[_ <: R]] {
      override def call(t: T): rx.Observable[_ <: R] = onNext(t).asJavaObservable
    }
    val jOnError = new Func1[Throwable, rx.Observable[_ <: R]] {
      override def call(e: Throwable): rx.Observable[_ <: R] = onError(e).asJavaObservable
    }
    val jOnCompleted = new Func0[rx.Observable[_ <: R]] {
      override def call(): rx.Observable[_ <: R] = onCompleted().asJavaObservable
    }
    toScalaObservable[R](asJavaObservable.flatMap[R](jOnNext, jOnError, jOnCompleted, maxConcurrent))
  }

  /**
   * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
   * source Observable and a specified collection Observable.
   *
   * <img width="640" height="390" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="" />
   *
   * @tparam U the type of items emitted by the collection Observable
   * @tparam R the type of items emitted by the resulting Observable
   * @param collectionSelector a function that returns an Observable for each item emitted by the source Observable
   * @param resultSelector a function that combines one item emitted by each of the source and collection Observables and
   *                       returns an item to be emitted by the resulting Observable
   * @return an Observable that emits the results of applying a function to a pair of values emitted by the
   *         source Observable and the collection Observable
   */
  def flatMapWith[U, R](collectionSelector: T => Observable[U])(resultSelector: (T, U) => R): Observable[R] = {
    val jCollectionSelector = new Func1[T, rx.Observable[_ <: U]] {
      override def call(t: T): rx.Observable[_ <: U] = collectionSelector(t).asJavaObservable
    }
    toScalaObservable[R](asJavaObservable.flatMap[U, R](jCollectionSelector, resultSelector))
  }

  /**
   * $beta Returns an Observable that emits the results of a specified function to the pair of values emitted by the
   * source Observable and a specified collection Observable.
   *
   * <img width="640" height="390" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="" />
   *
   * @tparam U the type of items emitted by the collection Observable
   * @tparam R the type of items emitted by the resulting Observable
   * @param maxConcurrent the maximum number of Observables that may be subscribed to concurrently
   * @param collectionSelector a function that returns an Observable for each item emitted by the source Observable
   * @param resultSelector a function that combines one item emitted by each of the source and collection Observables and
   *                       returns an item to be emitted by the resulting Observable
   * @return an Observable that emits the results of applying a function to a pair of values emitted by the
   *         source Observable and the collection Observable
   */
  @Beta
  def flatMapWith[U, R](maxConcurrent: Int, collectionSelector: T => Observable[U])(resultSelector: (T, U) => R): Observable[R] = {
    val jCollectionSelector = new Func1[T, rx.Observable[_ <: U]] {
      override def call(t: T): rx.Observable[_ <: U] = collectionSelector(t).asJavaObservable
    }
    toScalaObservable[R](asJavaObservable.flatMap[U, R](jCollectionSelector, resultSelector, maxConcurrent))
  }

  /**
   * Returns an Observable that merges each item emitted by the source Observable with the values in an
   * Iterable corresponding to that item that is generated by a selector.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMapIterable.png" alt="" />
   *
   * @tparam R the type of item emitted by the resulting Observable
   * @param collectionSelector a function that returns an Iterable sequence of values for when given an item emitted by the
   *                           source Observable
   * @return an Observable that emits the results of merging the items emitted by the source Observable with
   *         the values in the Iterables corresponding to those items, as generated by `collectionSelector`
   */
  def flatMapIterable[R](collectionSelector: T => Iterable[R]): Observable[R] = {
    val jCollectionSelector = new Func1[T, java.lang.Iterable[_ <: R]] {
      override def call(t: T): java.lang.Iterable[_ <: R] = collectionSelector(t).asJava
    }
    toScalaObservable[R](asJavaObservable.flatMapIterable[R](jCollectionSelector))
  }

  /**
   * Returns an Observable that emits the results of applying a function to the pair of values from the source
   * Observable and an Iterable corresponding to that item that is generated by a selector.
   *
   * <img width="640" height="390" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMapIterable.r.png" alt="" />
   *
   * @tparam U the collection element type
   * @tparam R the type of item emited by the resulting Observable
   * @param collectionSelector a function that returns an Iterable sequence of values for each item emitted by the source
   *                           Observable
   * @param resultSelector a function that returns an item based on the item emitted by the source Observable and the
   *                       Iterable returned for that item by the `collectionSelector`
   * @return an Observable that emits the items returned by `resultSelector` for each item in the source Observable
   */
  def flatMapIterableWith[U, R](collectionSelector: T => Iterable[U])(resultSelector: (T, U) => R): Observable[R] = {
    val jCollectionSelector = new Func1[T, java.lang.Iterable[_ <: U]] {
      override def call(t: T): java.lang.Iterable[_ <: U] = collectionSelector(t).asJava
    }
    toScalaObservable[R](asJavaObservable.flatMapIterable[U, R](jCollectionSelector, resultSelector))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable and emits the result.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="" />
   *
   * @param func
   *            a function to apply to each item emitted by the Observable
   * @return an Observable that emits the items from the source Observable, transformed by the
   *         given function
   */
  def map[R](func: T => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.map[R](new Func1[T,R] {
      def call(t1: T): R = func(t1)
    }))
  }

  /**
   * Turns all of the notifications from a source Observable into [[rx.lang.scala.Observer.onNext onNext]] emissions,
   * and marks them with their original notification types within [[rx.lang.scala.Notification]] objects.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.png" alt="" />
   *
   * @return an Observable whose items are the result of materializing the items and
   *         notifications of the source Observable
   */
  def materialize: Observable[Notification[T]] = {
    toScalaObservable[rx.Notification[_ <: T]](asJavaObservable.materialize()).map(Notification(_))
  }

  /**
   * Asynchronously subscribes and unsubscribes Observers on the specified [[rx.lang.scala.Scheduler]].
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/subscribeOn.png" alt="" />
   *
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to perform subscription and unsubscription actions on
   * @return the source Observable modified so that its subscriptions and unsubscriptions happen
   *         on the specified [[rx.lang.scala.Scheduler]]
   */
  def subscribeOn(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.subscribeOn(scheduler))
  }

  /**
   * Asynchronously unsubscribes on the specified [[Scheduler]].
   *
   * @param scheduler the [[Scheduler]] to perform subscription and unsubscription actions on
   * @return the source Observable modified so that its unsubscriptions happen on the specified [[Scheduler]]
   * @since 0.17
   */
  def unsubscribeOn(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.unsubscribeOn(scheduler))
  }

  /**
   * Asynchronously notify [[rx.lang.scala.Observer]]s on the specified [[rx.lang.scala.Scheduler]].
   *
   * <img width="640" height="308" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.png" alt="" />
   *
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to notify [[rx.lang.scala.Observer]]s on
   * @return the source Observable modified so that its [[rx.lang.scala.Observer]]s are notified on the
   *         specified [[rx.lang.scala.Scheduler]]
   */
  def observeOn(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.observeOn(scheduler))
  }

  /**
   * Returns an Observable that reverses the effect of [[rx.lang.scala.Observable.materialize]] by
   * transforming the [[rx.lang.scala.Notification]] objects emitted by the source Observable into the items
   * or notifications they represent.
   *
   * This operation is only available if `this` is of type `Observable[Notification[U]]` for some `U`, 
   * otherwise you will get a compilation error.
   *
   * <img width="640" height="335" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/dematerialize.png" alt="" />
   *
   * @return an Observable that emits the items and notifications embedded in the [[rx.lang.scala.Notification]] objects emitted by the source Observable
   *
   * @usecase def dematerialize[U]: Observable[U]
   *   @inheritdoc
   *
   */
  // with =:= it does not work, why?
  def dematerialize[U](implicit evidence: Observable[T] <:< Observable[Notification[U]]): Observable[U] = {
    val o1: Observable[Notification[U]] = this
    val o2: Observable[rx.Notification[_ <: U]] = o1.map(_.asJavaNotification)
    val o3 = o2.asJavaObservable.dematerialize[U]()
    toScalaObservable[U](o3)
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="" />
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass a
   * function that returns an Observable (`resumeFunction`) to
   * `onErrorResumeNext`, if the original Observable encounters an error, instead of
   * invoking its Observer's `onError` method, it will instead relinquish control to
   * the Observable returned from `resumeFunction`, which will invoke the Observer's 
   * [[rx.lang.scala.Observer.onNext onNext]] method if it is able to do so. In such a case, because no
   * Observable necessarily invokes `onError`, the Observer may never know that an
   * error happened.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeFunction
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  def onErrorResumeNext[U >: T](resumeFunction: Throwable => Observable[U]): Observable[U] = {
    val f: Func1[Throwable, rx.Observable[_ <: U]] = (t: Throwable) => resumeFunction(t).asJavaObservable
    val f2 = f.asInstanceOf[Func1[Throwable, rx.Observable[Nothing]]]
    toScalaObservable[U](asJavaObservable.onErrorResumeNext(f2))
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error of type `java.lang.Exception`.
   *
   * This differs from `Observable.onErrorResumeNext` in that this one does not handle `java.lang.Throwable` or `java.lang.Error` but lets those continue through.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="" />
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass
   * another Observable (`resumeSequence`) to an Observable's
   * `onErrorResumeNext` method, if the original Observable encounters an error,
   * instead of invoking its Observer's `onError` method, it will instead relinquish
   * control to `resumeSequence` which will invoke the Observer's [[rx.lang.scala.Observer.onNext onNext]]
   * method if it is able to do so. In such a case, because no
   * Observable necessarily invokes `onError`, the Observer may never know that an
   * error happened.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeSequence
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  def onExceptionResumeNext[U >: T](resumeSequence: Observable[U]): Observable[U] = {
    val rSeq1: rx.Observable[_ <: U] = resumeSequence.asJavaObservable
    val rSeq2: rx.Observable[Nothing] = rSeq1.asInstanceOf[rx.Observable[Nothing]]
    toScalaObservable[U](asJavaObservable.onExceptionResumeNext(rSeq2))
  }

  /**
   * Instruct an Observable to emit an item (returned by a specified function) rather than
   * invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturn.png" alt="" />
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorReturn` method changes this behavior. If you pass a function
   * (`resumeFunction`) to an Observable's `onErrorReturn` method, if the
   * original Observable encounters an error, instead of invoking its Observer's
   * `onError` method, it will instead pass the return value of
   * `resumeFunction` to the Observer's [[rx.lang.scala.Observer.onNext onNext]] method.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeFunction
   *            a function that returns an item that the new Observable will emit if the source
   *            Observable encounters an error
   * @return the original Observable with appropriately modified behavior
   */
  def onErrorReturn[U >: T](resumeFunction: Throwable => U): Observable[U] = {
    val f1: Func1[Throwable, _ <: U] = resumeFunction
    val f2 = f1.asInstanceOf[Func1[Throwable, Nothing]]
    toScalaObservable[U](asJavaObservable.onErrorReturn(f2))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by the source Observable into the same function, and so on until all items have been emitted
   * by the source Observable, and emits the final result from the final call to your function as
   * its sole item.
   *
   * <img width="640" height="320" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/reduce.png" alt="" />
   *
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an `inject` method that does a similar operation on lists.
   *
   * @param accumulator
   *            An accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the
   *         output from the source Observable
   */
  def reduce[U >: T](accumulator: (U, U) => U): Observable[U] = {
    val func: Func2[_ >: U, _ >: U, _ <: U] = accumulator
    val func2 = func.asInstanceOf[Func2[T, T, T]]
    toScalaObservable[U](asJavaObservable.asInstanceOf[rx.Observable[T]].reduce(func2))
  }

  /**
   * Returns a [[rx.lang.scala.observables.ConnectableObservable]] that shares a single subscription to the underlying
   * Observable that will replay all of its items and notifications to any future [[rx.lang.scala.Observer]].
   *
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.png" alt="" />
   *
   * @return a [[rx.lang.scala.observables.ConnectableObservable]] such that when the `connect` function
   *         is called, the [[rx.lang.scala.observables.ConnectableObservable]] starts to emit items to its [[rx.lang.scala.Observer]]s
   */
  def replay: ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay())
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on the items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable.
   * <p>
   * <img width="640" height="450" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.f.png" alt="" />
   *
   * @param selector the selector function, which can use the multicasted sequence as many times as needed, without
   *                 causing multiple subscriptions to the Observable
   * @return an Observable that emits items that are the results of invoking the selector on a `ConnectableObservable`
   *         that shares a single subscription to the source Observable
   */
  def replay[R](selector: Observable[T] => Observable[R]): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying `bufferSize` notifications.
   * <p>
   * <img width="640" height="440" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.fn.png" alt="" />
   *
   * @param selector the selector function, which can use the multicasted sequence as many times as needed, without
   *                 causing multiple subscriptions to the Observable
   * @param bufferSize  the buffer size that limits the number of items the connectable observable can replay
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable  replaying
   *         no more than `bufferSize` items
   */
  def replay[R](selector: Observable[T] => Observable[R], bufferSize: Int): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying no more than `bufferSize` items that were emitted within a specified time window.
   * <p>
   * <img width="640" height="445" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.fnt.png" alt="" />
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize the buffer size that limits the number of items the connectable observable can replay
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable, and
   *         replays no more than `bufferSize` items that were emitted within the window defined by `time`
   */
  def replay[R](selector: Observable[T] => Observable[R], bufferSize: Int, time: Duration): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, time.length, time.unit))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying no more than `bufferSize` items that were emitted within a specified time window.
   * <p>
   * <img width="640" height="445" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.fnts.png" alt="" />
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize the buffer size that limits the number of items the connectable observable can replay
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @param scheduler  the Scheduler that is the time source for the window
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable, and
   *         replays no more than `bufferSize` items that were emitted within the window defined by `time`
   * @throws java.lang.IllegalArgumentException if `bufferSize` is less than zero
   */
  def replay[R](selector: Observable[T] => Observable[R], bufferSize: Int, time: Duration, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying a maximum of `bufferSize` items.
   * <p>
   * <img width="640" height="440" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.fns.png" alt="" />
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize  the buffer size that limits the number of items the connectable observable can replay
   * @param scheduler  the Scheduler on which the replay is observed
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying no more than `bufferSize` notifications
   */
  def replay[R](selector: Observable[T] => Observable[R], bufferSize: Int, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying all items that were emitted within a specified time window.
   * <p>
   * <img width="640" height="435" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.ft.png" alt="" />
   *
   * @param selector   a selector function, which can use the multicasted sequence as many times as needed, without
   *                   causing multiple subscriptions to the Observable
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items that were emitted within the window defined by `time`
   */
  def replay[R](selector: Observable[T] => Observable[R], time: Duration, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable.
   * <p>
   * <img width="640" height="445" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.fs.png" alt="" />
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param scheduler    the Scheduler where the replay is observed
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items
   */
  def replay[R](selector: Observable[T] => Observable[R], scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays at most `bufferSize` items that were emitted during a specified time window.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.nt.png" alt="" />
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted during the window defined by `time`
   */
  def replay(bufferSize: Int, time: Duration): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * that replays a maximum of `bufferSize` items that are emitted within a specified time window.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.nts.png" alt="" />
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param time the duration of the window in which the replayed items must have been emitted
   * @param scheduler the scheduler that is used as a time source for the window
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted during the window defined by `time`
   *@throws java.lang.IllegalArgumentException if `bufferSize` is less than zero
   */
  def replay(bufferSize: Int, time: Duration, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying all items that were emitted within a specified time window.
   * <p>
   * <img width="640" height="435" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.ft.png" alt="" />
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items that were emitted within the window defined by `time`
   */
  def replay[R](selector: Observable[T] => Observable[R], time: Duration): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable that
   * replays at most `bufferSize` items emitted by that Observable.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.n.png" alt="" />
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items emitted by that Observable
   */
  def replay(bufferSize: Int): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays at most `bufferSize` items emitted by that Observable.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.ns.png" alt="" />
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param scheduler the scheduler on which the Observers will observe the emitted items
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted by the Observable
   */
  def replay(bufferSize: Int, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays all items emitted by that Observable within a specified time window.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.t.png" alt="" />
   *
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays the items that were emitted during the window defined by `time`
   */
  def replay(time: Duration): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays all items emitted by that Observable within a specified time window.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.ts.png" alt="" />
   *
   * @param time the duration of the window in which the replayed items must have been emitted
   * @param scheduler the Scheduler that is the time source for the window
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays the items that were emitted during the window defined by `time`
   */
  def replay(time: Duration, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(time.length, time.unit, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable that
   * will replay all of its items and notifications to any future `Observer` on the given `Scheduler`.
   * <p>
   * <img width="640" height="515" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.s.png" alt="" />
   *
   * @param scheduler the Scheduler on which the Observers will observe the emitted items
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable that
   *         will replay all of its items and notifications to any future `bserver` on the given `Scheduler`
   */
  def replay(scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(scheduler))
  }

  /**
   * This method has similar behavior to `Observable.replay` except that this auto-subscribes to
   * the source Observable rather than returning a start function and an Observable.
   *
   * <img width="640" height="410" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/cache.png" alt="" />
   *
   * This is useful when you want an Observable to cache responses and you can't control the
   * subscribe/unsubscribe behavior of all the [[rx.lang.scala.Observer]]s.
   *
   * When you call `cache`, it does not yet subscribe to the
   * source Observable. This only happens when `subscribe` is called
   * the first time on the Observable returned by `cache`.
   * 
   * Note: You sacrifice the ability to unsubscribe from the origin when you use the
   * `cache()` operator so be careful not to use this operator on Observables that
   * emit an infinite or very large number of items that will use up memory.
   *
   * @return an Observable that when first subscribed to, caches all of its notifications for
   *         the benefit of subsequent subscribers.
   */
  def cache: Observable[T] = {
    toScalaObservable[T](asJavaObservable.cache())
  }

  /**
   * Caches emissions from the source Observable and replays them in order to any subsequent Subscribers.
   * This method has similar behavior to `Observable.replay` except that this auto-subscribes to the source
   * Observable rather than returning a [[rx.lang.scala.observables.ConnectableObservable ConnectableObservable]] for which you must call
   * `connect` to activate the subscription.
   * <p>
   * <img width="640" height="410" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/cache.png" alt="" />
   * <p>
   * This is useful when you want an Observable to cache responses and you can't control the
   * `subscribe/unsubscribe` behavior of all the [[Subscriber]]s.
   * <p>
   * When you call `cache`, it does not yet subscribe to the source Observable and so does not yet
   * begin cacheing items. This only happens when the first Subscriber calls the resulting Observable's
   * `subscribe` method.
   * <p>
   * <em>Note:</em> You sacrifice the ability to unsubscribe from the origin when you use the `cache`
   * Observer so be careful not to use this Observer on Observables that emit an infinite or very large number
   * of items that will use up memory.
   *
   * ===Backpressure Support:===
   * This operator does not support upstream backpressure as it is purposefully requesting and caching everything emitted.
   *
   * ===Scheduler:===
   * `cache` does not operate by default on a particular `Scheduler`.
   *
   * @param capacity hint for number of items to cache (for optimizing underlying data structure)
   * @return an Observable that, when first subscribed to, caches all of its items and notifications for the
   *         benefit of subsequent subscribers
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#cache">RxJava wiki: cache</a>
   * @since 0.20
   */
  def cache(capacity: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.cache(capacity))
  }

  /**
   * Returns a new [[Observable]] that multicasts (shares) the original [[Observable]]. As long a
   * there is more than 1 [[Subscriber]], this [[Observable]] will be subscribed and emitting data.
   * When all subscribers have unsubscribed it will unsubscribe from the source [[Observable]].
   *
   * This is an alias for `publish().refCount()`
   *
   * <img width="640" height="510" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/publishRefCount.png" alt="" />
   *
   * @return a [[Observable]] that upon connection causes the source Observable to emit items to its [[Subscriber]]s
   * @since 0.19
   */
  def share: Observable[T] = {
    toScalaObservable[T](asJavaObservable.share())
  }

  /**
   * Returns an Observable that emits a Boolean that indicates whether the source Observable emitted a
   * specified item.
   *
   * Note: this method uses `==` to compare elements. It's a bit different from RxJava which uses `Object.equals`.
   * <p>
   * <img width="640" height="320" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/contains.png" alt="" />
   *
   *@param elem the item to search for in the emissions from the source Observable
   * @return an Observable that emits `true` if the specified item is emitted by the source Observable,
   *         or `false` if the source Observable completes without emitting that item
   */
  def contains[U >: T](elem: U): Observable[Boolean] = {
    exists(_ == elem)
  }

  /**
   * Returns a [[rx.lang.scala.observables.ConnectableObservable]], which waits until the `connect` function is called
   * before it begins emitting items from `this` [[rx.lang.scala.Observable]] to those [[rx.lang.scala.Observer]]s that
   * have subscribed to it.
   *
   * <img width="640" height="510" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/publishConnect.png" alt="" />
   *
   * @return an [[rx.lang.scala.observables.ConnectableObservable]].
   */
  def publish: ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.publish())
  }

  /**
   * Returns an Observable that emits the results of invoking a specified selector on items emitted by a `ConnectableObservable`
   * that shares a single subscription to the underlying sequence.
   * <p>
   * <img width="640" height="510" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/publishConnect.f.png" alt="" />
   *
   * @param selector a function that can use the multicasted source sequence as many times as needed, without
   *                 causing multiple subscriptions to the source sequence. Subscribers to the given source will
   *                 receive all notifications of the source from the time of the subscription forward.
   * @return an Observable that emits the results of invoking the selector on the items emitted by a `ConnectableObservable`
   *         that shares a single subscription to the underlying sequence
   */
  def publish[R](selector: Observable[T] => Observable[R]): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => selector(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.publish(fJava))
  }

  // TODO add Scala-like aggregate function

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the final result from the final call to your function as its sole
   * item.
   *
   * <img width="640" height="325" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/reduceSeed.png" alt="" />
   *
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an `inject` method that does a similar operation on lists.
   *
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, the result of which will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the output
   *         from the items emitted by the source Observable
   */
  def foldLeft[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.reduce(initialValue, new Func2[R,T,R]{
      def call(t1: R, t2: T): R = accumulator(t1,t2)
    }))
  }

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.png" alt="" />
   *
   * @param duration the sampling rate
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(duration.length, duration.unit))
  }

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.png" alt="" />
   *
   * @param duration the sampling rate
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to use when sampling
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(duration.length, duration.unit, scheduler))
  }

  /**
   * Return an Observable that emits the results of sampling the items emitted by the source Observable
   * whenever the specified sampler Observable emits an item or completes.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.o.png" alt="" />
   *
   * @param sampler
   *            the Observable to use for sampling the source Observable
   * @return an Observable that emits the results of sampling the items emitted by this Observable whenever
   *         the sampler Observable emits an item or completes
   */
  def sample(sampler: Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(sampler))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the result of each of these iterations.
   *
   * <img width="640" height="320" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.png" alt="" />
   *
   * This sort of function is sometimes called an accumulator.
   *
   * Note that when you pass a seed to `scan()` the resulting Observable will emit
   * that seed as its first emitted item.
   *
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be emitted to [[rx.lang.scala.Observer]]s via
   *            [[rx.lang.scala.Observer.onNext onNext]] and used in the next accumulator call.
   * @return an Observable that emits the results of each call to the accumulator function
   */
  def scan[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.scan(initialValue, new Func2[R,T,R]{
      def call(t1: R, t2: T): R = accumulator(t1,t2)
    }))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the
   * first item emitted by a source Observable, then feeds the result of that
   * function along with the second item emitted by an Observable into the
   * same function, and so on until all items have been emitted by the source
   * Observable, emitting the result of each of these iterations.
   * <p>
   * <img width="640" height="320" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/scan.png" alt="" />
   * <p>
   *
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be emitted to [[rx.lang.scala.Observer]]s via
   *            [[rx.lang.scala.Observer.onNext onNext]] and used in the next accumulator call.
   * @return
   *         an Observable that emits the results of each call to the
   *         accumulator function
   */
  def scan[U >: T](accumulator: (U, U) => U): Observable[U] = {
    val func: Func2[_ >: U, _ >: U, _ <: U] = accumulator
    val func2 = func.asInstanceOf[Func2[T, T, T]]
    toScalaObservable[U](asJavaObservable.asInstanceOf[rx.Observable[T]].scan(func2))
  }

  /**
   * Returns an Observable that emits a Boolean that indicates whether all of the items emitted by
   * the source Observable satisfy a condition.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/all.png" alt="" />
   *
   * @param predicate
   *            a function that evaluates an item and returns a Boolean
   * @return an Observable that emits `true` if all items emitted by the source
   *         Observable satisfy the predicate; otherwise, `false`
   */
  def forall(predicate: T => Boolean): Observable[Boolean] = {
    toScalaObservable[java.lang.Boolean](asJavaObservable.all(predicate)).map(_.booleanValue())
  }

  /**
   * Returns an Observable that skips the first `num` items emitted by the source
   * Observable and emits the remainder.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.png" alt="" />
   *
   * @param n
   *            the number of items to skip
   * @return an Observable that is identical to the source Observable except that it does not
   *         emit the first `num` items that the source emits
   */
  def drop(n: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.skip(n))
  }

  /**
   * Returns an Observable that drops values emitted by the source Observable before a specified time window
   * elapses.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.t.png" alt="" />
   *
   * @param time the length of the time window to drop
   * @return an Observable that drops values emitted by the source Observable before the time window defined
   *         by `time` elapses and emits the remainder
   */
  def drop(time: Duration): Observable[T] = {
    toScalaObservable(asJavaObservable.skip(time.length, time.unit))
  }

  /**
   * Returns an Observable that drops values emitted by the source Observable before a specified time window
   * elapses.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.t.png" alt="" />
   *
   * @param time the length of the time window to drop
   * @param scheduler the `Scheduler` on which the timed wait happens
   * @return an Observable that drops values emitted by the source Observable before the time window defined
   *         by `time` elapses and emits the remainder
   */
  def drop(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable(asJavaObservable.skip(time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that bypasses all items from the source Observable as long as the specified
   * condition holds true. Emits all further source items as soon as the condition becomes false.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skipWhile.png" alt="" />
   *
   * @param predicate
   *            A function to test each item emitted from the source Observable for a condition.
   * @return an Observable that emits all items from the source Observable as soon as the condition
   *         becomes false.
   */
  def dropWhile(predicate: T => Boolean): Observable[T] = {
    toScalaObservable(asJavaObservable.skipWhile(predicate))
  }

  /**
   * Returns an Observable that drops a specified number of items from the end of the sequence emitted by the
   * source Observable.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.png" alt="" />
   * <p>
   * This Observer accumulates a queue long enough to store the first `n` items. As more items are
   * received, items are taken from the front of the queue and emitted by the returned Observable. This causes
   * such items to be delayed.
   *
   * @param n number of items to drop from the end of the source sequence
   * @return an Observable that emits the items emitted by the source Observable except for the dropped ones
   *         at the end
   * @throws java.lang.IndexOutOfBoundsException if `n` is less than zero
   */
  def dropRight(n: Int): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(n))
  }

  /**
   * Returns an Observable that drops items emitted by the source Observable during a specified time window
   * before the source completes.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.t.png" alt="" />
   *
   * Note: this action will cache the latest items arriving in the specified time window.
   *
   * @param time the length of the time window
   * @return an Observable that drops those items emitted by the source Observable in a time window before the
   *         source completes defined by `time`
   */
  def dropRight(time: Duration): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(time.length, time.unit))
  }

  /**
   * Returns an Observable that drops items emitted by the source Observable during a specified time window
   * (defined on a specified scheduler) before the source completes.
   * <p>
   * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.png" alt="" />
   *
   * Note: this action will cache the latest items arriving in the specified time window.
   *
   * @param time the length of the time window
   * @param scheduler the scheduler used as the time source
   * @return an Observable that drops those items emitted by the source Observable in a time window before the
   *         source completes defined by `time` and `scheduler`
   */
  def dropRight(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that skips items emitted by the source Observable until a second Observable emits an item.
   * <p>
   * <img width="640" height="375" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/skipUntil.png" alt="" />
   *
   * @param other the second Observable that has to emit an item before the source Observable's elements begin
   *              to be mirrored by the resulting Observable
   * @return an Observable that skips items from the source Observable until the second Observable emits an
   *         item, then emits the remaining items
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#wiki-skipuntil">RxJava Wiki: skipUntil()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229358.aspx">MSDN: Observable.SkipUntil</a>
   */
  def dropUntil(other: Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.skipUntil(other))
  }

  /**
   * Returns an Observable that emits only the first `num` items emitted by the source
   * Observable.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/take.png" alt="" />
   *
   * This method returns an Observable that will invoke a subscribing [[rx.lang.scala.Observer]]'s 
   * [[rx.lang.scala.Observer.onNext onNext]] function a maximum of `num` times before invoking
   * [[rx.lang.scala.Observer.onCompleted onCompleted]].
   *
   * @param n
   *            the number of items to take
   * @return an Observable that emits only the first `num` items from the source
   *         Observable, or all of the items from the source Observable if that Observable emits
   *         fewer than `num` items
   */
  def take(n: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.take(n))
  }

  /**
   * Returns an Observable that emits those items emitted by source Observable before a specified time runs out.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/take.t.png" alt="" />
   *
   * @param time the length of the time window
   * @return an Observable that emits those items emitted by the source Observable before the time runs out
   */
  def take(time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.take(time.length, time.unit))
  }

  /**
   * Returns an Observable that emits those items emitted by source Observable before a specified time (on
   * specified Scheduler) runs out
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/take.ts.png" alt="" />
   *
   * @param time the length of the time window
   * @param scheduler the Scheduler used for time source
   * @return an Observable that emits those items emitted by the source Observable before the time runs out,
   *         according to the specified Scheduler
   */
  def take(time: Duration, scheduler: Scheduler) {
    toScalaObservable[T](asJavaObservable.take(time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * $experimental Returns an [[Observable]] that emits items emitted by the source [[Observable]], checks the specified predicate
   * for each item, and then completes if the condition is satisfied.
   *
   * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.p.png" alt="">
   *
   * The difference between this operator and `takeWhile(T => Boolean)` is that here, the condition is
   * evaluated '''after''' the item is emitted.
   *
   * $noDefaultScheduler
   *
   * @param stopPredicate a function that evaluates an item emitted by the source [[Observable]] and returns a Boolean
   * @return an [[Observable]] that first emits items emitted by the source [[Observable]], checks the specified
   *         condition after each item, and then completes if the condition is satisfied.
   * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
   * @see [[Observable.takeWhile]]
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def takeUntil(stopPredicate: T => Boolean): Observable[T] = {
    val func = new Func1[T, java.lang.Boolean] {
      override def call(t: T): java.lang.Boolean = stopPredicate(t)
    }
    toScalaObservable[T](asJavaObservable.takeUntil(func))
  }

  /**
   * Returns an Observable that emits items emitted by the source Observable so long as a
   * specified condition is true.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeWhile.png" alt="" />
   *
   * @param predicate
   *            a function that evaluates an item emitted by the source Observable and returns a
   *            Boolean
   * @return an Observable that emits the items from the source Observable so long as each item
   *         satisfies the condition defined by `predicate`
   */
  def takeWhile(predicate: T => Boolean): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeWhile(predicate))
  }

  /**
   * Returns an Observable that emits only the last `count` items emitted by the source
   * Observable.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/last.png" alt="" />
   *
   * @param count
   *            the number of items to emit from the end of the sequence emitted by the source
   *            Observable
   * @return an Observable that emits only the last `count` items emitted by the source
   *         Observable
   */
  def takeRight(count: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count))
  }

  /**
   * Return an Observable that emits the items from the source Observable that were emitted in a specified
   * window of `time` before the Observable completed.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.t.png" alt="" />
   *
   * @param time the length of the time window
   * @return an Observable that emits the items from the source Observable that were emitted in the window of
   *         time before the Observable completed specified by `time`
   */
  def takeRight(time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(time.length, time.unit))
  }

  /**
   * Return an Observable that emits the items from the source Observable that were emitted in a specified
   * window of `time` before the Observable completed, where the timing information is provided by a specified
   * Scheduler.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.png" alt="" />
   *
   * @param time the length of the time window
   * @param scheduler the Scheduler that provides the timestamps for the Observed items
   * @return an Observable that emits the items from the source Observable that were emitted in the window of
   *         time before the Observable completed specified by `time`, where the timing information is
   *         provided by `scheduler`
   */
  def takeRight(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Return an Observable that emits at most a specified number of items from the source Observable that were
   * emitted in a specified window of time before the Observable completed.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tn.png" alt="" />
   *
   * @param count the maximum number of items to emit
   * @param time the length of the time window
   * @return an Observable that emits at most `count` items from the source Observable that were emitted
   *         in a specified window of time before the Observable completed
   * @throws java.lang.IllegalArgumentException if `count` is less than zero
   */
  def takeRight(count: Int, time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count, time.length, time.unit))
  }

  /**
   * Return an Observable that emits at most a specified number of items from the source Observable that were
   * emitted in a specified window of `time` before the Observable completed, where the timing information is
   * provided by a given Scheduler.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tns.png" alt="" />
   *
   * @param count the maximum number of items to emit
   * @param time the length of the time window
   * @param scheduler the Scheduler that provides the timestamps for the observed items
   * @return an Observable that emits at most `count` items from the source Observable that were emitted
   *         in a specified window of time before the Observable completed, where the timing information is
   *         provided by the given `scheduler`
   * @throws java.lang.IllegalArgumentException if `count` is less than zero
   */
  def takeRight(count: Int, time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count, time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Returns an Observable that emits the items from the source Observable only until the
   * `other` Observable emits an item.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.png" alt="" />
   *
   * @param that
   *            the Observable whose first emitted item will cause `takeUntil` to stop
   *            emitting items from the source Observable
    * @return an Observable that emits the items of the source Observable until such time as
   *         `other` emits its first item
   */
  def takeUntil(that: Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeUntil(that.asJavaObservable))
  }

  /**
   * Returns an Observable that emits a single item, a list composed of all the items emitted by
   * the source Observable.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.png" alt="" />
   *
   * Normally, an Observable that returns multiple items will do so by invoking its [[rx.lang.scala.Observer]]'s 
   * [[rx.lang.scala.Observer.onNext onNext]] method for each such item. You can change
   * this behavior, instructing the Observable to compose a list of all of these items and then to
   * invoke the Observer's `onNext` function once, passing it the entire list, by
   * calling the Observable's `toList` method prior to calling its `Observable.subscribe` method.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item: a List containing all of the items emitted by
   *         the source Observable.
   */
  def toSeq: Observable[Seq[T]] = {
    Observable.jObsOfListToScObsOfSeq(asJavaObservable.toList)
      : Observable[Seq[T]] // SI-7818
  }

  /**
   * Groups the items emitted by this Observable according to a specified discriminator function.
   *
   * @param f
   *            a function that extracts the key from an item
   * @tparam K
   *            the type of keys returned by the discriminator function.
   * @return an Observable that emits `(key, observable)` pairs, where `observable`
   *         contains all items for which `f` returned `key`.
   */
  def groupBy[K](f: T => K): Observable[(K, Observable[T])] = {
    val o1 = asJavaObservable.groupBy[K](f) : rx.Observable[_ <: rx.observables.GroupedObservable[K, _ <: T]]
    val func = (o: rx.observables.GroupedObservable[K, _ <: T]) => (o.getKey, toScalaObservable[T](o))
    toScalaObservable[(K, Observable[T])](o1.map[(K, Observable[T])](func))
  }

  /**
   * Groups the items emitted by an [[Observable]] according to a specified criterion, and emits these
   * grouped items as `(key, observable)` pairs.
   *
   * <img width="640" height="360" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="" />
   *
   * Note: A `(key, observable)` will cache the items it is to emit until such time as it
   * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
   * `(key, observable)` pairs that do not concern you. Instead, you can signal to them that they may
   * discard their buffers by applying an operator like `take(0)` to them.
   *
   * ===Backpressure Support:===
   * This operator does not support backpressure as splitting a stream effectively turns it into a "hot observable"
   * and blocking any one group would block the entire parent stream. If you need backpressure on individual groups
   * then you should use operators such as `nBackpressureDrop` or `@link #onBackpressureBuffer`.</dd>
   * ===Scheduler:===
   * groupBy` does not operate by default on a particular `Scheduler`.
   *
   * @param keySelector a function that extracts the key for each item
   * @param valueSelector a function that extracts the return element for each item
   * @tparam K the key type
   * @tparam V the value type
   * @return an [[Observable]] that emits `(key, observable)` pairs, each of which corresponds to a
   *         unique key value and each of which emits those items from the source Observable that share that
   *         key value
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#groupby-and-groupbyuntil">RxJava wiki: groupBy</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.groupby.aspx">MSDN: Observable.GroupBy</a>
   */
  def groupBy[K, V](keySelector: T => K, valueSelector: T => V): Observable[(K, Observable[V])] = {
    val jo: rx.Observable[rx.observables.GroupedObservable[K, V]] = asJavaObservable.groupBy[K, V](keySelector, valueSelector)
    toScalaObservable[rx.observables.GroupedObservable[K, V]](jo).map {
      go: rx.observables.GroupedObservable[K, V] => (go.getKey, toScalaObservable[V](go))
    }
  }

  /**
   * Correlates the items emitted by two Observables based on overlapping durations.
   * <p>
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/join_.png" alt="" />
   *
   * @param other
   *          the second Observable to join items from
   * @param leftDurationSelector
   *          a function to select a duration for each item emitted by the source Observable,
   *          used to determine overlap
   * @param rightDurationSelector
   *         a function to select a duration for each item emitted by the inner Observable,
   *         used to determine overlap
   * @param resultSelector
   *         a function that computes an item to be emitted by the resulting Observable for any
   *         two overlapping items emitted by the two Observables
   * @return
   *         an Observable that emits items correlating to items emitted by the source Observables
   *         that have overlapping durations
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#join">RxJava Wiki: join()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229750.aspx">MSDN: Observable.Join</a>
   */
  def join[S, R] (other: Observable[S])(leftDurationSelector:  T => Observable[Any], rightDurationSelector: S => Observable[Any], resultSelector: (T, S) => R): Observable[R] = {
    val outer : rx.Observable[_ <: T] = this.asJavaObservable
    val inner : rx.Observable[_ <: S] = other.asJavaObservable
    val left:  Func1[_ >: T, _<: rx.Observable[_ <: Any]] =   (t: T) => leftDurationSelector(t).asJavaObservable
    val right: Func1[_ >: S, _<: rx.Observable[_ <: Any]] =  (s: S) => rightDurationSelector(s).asJavaObservable
    val f: Func2[_>: T, _ >: S, _ <: R] = resultSelector

    toScalaObservable[R](
      outer.asInstanceOf[rx.Observable[T]].join[S, Any, Any, R](
        inner.asInstanceOf[rx.Observable[S]],
        left. asInstanceOf[Func1[T, rx.Observable[Any]]],
        right.asInstanceOf[Func1[S, rx.Observable[Any]]],
        f.asInstanceOf[Func2[T,S,R]])
    )
  }

  /**
   * Returns an Observable that correlates two Observables when they overlap in time and groups the results.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/groupJoin.png" alt="" />
   *
   * @param other the other Observable to correlate items from the source Observable with
   * @param leftDuration a function that returns an Observable whose emissions indicate the duration of the values of
   *                     the source Observable
   * @param rightDuration a function that returns an Observable whose emissions indicate the duration of the values of
   *                      the `other` Observable
   * @param resultSelector a function that takes an item emitted by each Observable and returns the value to be emitted
   *                       by the resulting Observable
   * @return an Observable that emits items based on combining those items emitted by the source Observables
   *         whose durations overlap
   */
  def groupJoin[S, R](other: Observable[S])(leftDuration: T => Observable[Any], rightDuration: S => Observable[Any], resultSelector: (T, Observable[S]) => R): Observable[R] = {
    val outer: rx.Observable[_ <: T] = this.asJavaObservable
    val inner: rx.Observable[_ <: S] = other.asJavaObservable
    val left: Func1[_ >: T, _ <: rx.Observable[_ <: Any]] = (t: T) => leftDuration(t).asJavaObservable
    val right: Func1[_ >: S, _ <: rx.Observable[_ <: Any]] = (s: S) => rightDuration(s).asJavaObservable
    val f: Func2[_ >: T, _ >: rx.Observable[S], _ <: R] = (t: T, o: rx.Observable[S]) => resultSelector(t, toScalaObservable[S](o))
    toScalaObservable[R](
      outer.asInstanceOf[rx.Observable[T]].groupJoin[S, Any, Any, R](
        inner.asInstanceOf[rx.Observable[S]],
        left.asInstanceOf[Func1[T, rx.Observable[Any]]],
        right.asInstanceOf[Func1[S, rx.Observable[Any]]],
        f)
    )
  }

  /**
   * Returns a new Observable by applying a function that you supply to each item emitted by the source
   * Observable that returns an Observable, and then emitting the items emitted by the most recently emitted
   * of these Observables.
   *
   * <img width="640" height="350" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="" />
   *
   * @param f a function that, when applied to an item emitted by the source Observable, returns an Observable
   * @return an Observable that emits the items emitted by the Observable returned from applying a function to
   *         the most recently emitted item emitted by the source Observable
   */
  def switchMap[R](f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.switchMap[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t: T): rx.Observable[_ <: R] = f(t).asJavaObservable
    }))
  }

  /**
   * $experimental Returns an [[Observable]] that emits the items emitted by the source [[Observable]] or the items of an alternate
   * [[Observable]] if the source [[Observable]] is empty.
   *
   * $noDefaultScheduler
   *
   * @param alternate the alternate [[Observable]] to subscribe to if the source does not emit any items
   * @return an [[Observable]] that emits the items emitted by the source [[Observable]] or the items of an
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   *         alternate [[Observable]] if the source [[Observable]] is empty.
   */
  @Experimental
  def switchIfEmpty[U >: T](alternate: Observable[U]): Observable[U] = {
    val jo = asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](jo.switchIfEmpty(alternate.asJavaObservable))
  }

  /**
   * Given an Observable that emits Observables, creates a single Observable that
   * emits the items emitted by the most recently published of those Observables.
   *
   * <img width="640" height="370" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.png" alt="" />
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @return an Observable that emits only the items emitted by the most recently published
   *         Observable
   *
   * @usecase def switch[U]: Observable[U]
   *   @inheritdoc
   */
  def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.switchOnNext[U](o4)
    toScalaObservable[U](o5)
  }
  // Naming: We follow C# (switch), not Java (switchOnNext), because Java just had to avoid clash with keyword

  /**
   * Flattens two Observables into one Observable, without any transformation.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="" />
   *
   * You can combine items emitted by two Observables so that they act like a single
   * Observable by using the `merge` method.
   *
   * @param that
   *            an Observable to be merged
   * @return an Observable that emits items from `this` and `that` until 
   *            `this` or `that` emits `onError` or both Observables emit `onCompleted`.
   */
  def merge[U >: T](that: Observable[U]): Observable[U] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable[U](rx.Observable.merge(thisJava, thatJava))
  }

  /**
   * This behaves like [[rx.lang.scala.Observable.merge]] except that if any of the merged Observables
   * notify of an error via [[rx.lang.scala.Observer.onError onError]], `mergeDelayError` will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="" />
   *
   * Even if multiple merged Observables send `onError` notifications, `mergeDelayError` will only invoke the `onError` method of its
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
  def mergeDelayError[U >: T](that: Observable[U]): Observable[U] = {
    toScalaObservable[U](rx.Observable.mergeDelayError[U](this.asJavaObservable, that.asJavaObservable))
  }

  /**
   * Flattens the sequence of Observables emitted by `this` into one Observable, without any
   * transformation.
   *
   * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="" />
   *
   * You can combine the items emitted by multiple Observables so that they act like a single
   * Observable by using this method.
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @return an Observable that emits items that are the result of flattening the items emitted
   *         by the Observables emitted by `this`
   *
   * @usecase def flatten[U]: Observable[U]
   *   @inheritdoc
   */
  def flatten[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.merge[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * Flattens an Observable that emits Observables into a single Observable that emits the items emitted by
   * those Observables, without any transformation, while limiting the maximum number of concurrent
   * subscriptions to these Observables.
   *
   * <img width="640" height="370" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.oo.png" alt="" />
   *
   * You can combine the items emitted by multiple Observables so that they appear as a single Observable, by
   * using the `flatten` method.
   *
   * @param maxConcurrent the maximum number of Observables that may be subscribed to concurrently
   * @return an Observable that emits items that are the result of flattening the Observables emitted by the `source` Observable
   * @throws java.lang.IllegalArgumentException  if `maxConcurrent` is less than or equal to 0
   */
  def flatten[U](maxConcurrent: Int)(implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.merge[U](o4, maxConcurrent)
    toScalaObservable[U](o5)
  }

  /**
   * Flattens an [[Observable]] that emits [[Observable]]s into one [[Observable]], in a way that allows an [[Observer]] to
   * receive all successfully emitted items from all of the source [[Observable]]s without being interrupted by
   * an error notification from one of them, while limiting the
   * number of concurrent subscriptions to these [[Observable]]s.
   *
   * This behaves like `flatten` except that if any of the merged [[Observable]]s notify of an
   * error via `onError`, `flattenDelayError` will refrain from propagating that
   * error notification until all of the merged [[Observable]]s have finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
   *
   * Even if multiple merged [[Observable]]s send `onError` notifications, `flattenDelayError` will only
   * invoke the `onError` method of its `Observer`s once.
   *
   * $noDefaultScheduler
   *
   * @return an [[Observable]] that emits all of the items emitted by the [[Observable]]s emitted by `this`
   * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
   */
  def flattenDelayError[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
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
   * error via `onError`, `flattenDelayError` will refrain from propagating that
   * error notification until all of the merged [[Observable]]s have finished emitting items.
   *
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
   *
   * Even if multiple merged [[Observable]]s send `onError` notifications, `flattenDelayError` will only
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
  def flattenDelayError[U](maxConcurrent: Int)(implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.mergeDelayError[U](o4, maxConcurrent)
    toScalaObservable[U](o5)
  }

  /**
   * Combines two observables, emitting a pair of the latest values of each of
   * the source observables each time an event is received from one of the source observables.
   *
   * @param that
   *            The second source observable.
   * @return An Observable that combines the source Observables
   */
  def combineLatest[U](that: Observable[U]): Observable[(T, U)] = {
    val f: Func2[_ >: T, _ >: U, _ <: (T, U)] = (t: T, u: U) => (t, u)
    toScalaObservable[(T, U)](rx.Observable.combineLatest[T, U, (T, U)](this.asJavaObservable, that.asJavaObservable, f))
  }


  /**
   * Combines two observables, emitting some type `R` specified in the function `selector`,
   * each time an event is received from one of the source observables, where the aggregation
   * is defined by the given function.
   *
   * @param that The second source observable.
   * @param selector The function that is used combine the emissions of the two observables.
   * @return An Observable that combines the source Observables according to the function `selector`.
   */
  def combineLatestWith[U, R](that: Observable[U])(selector: (T, U) => R): Observable[R] = {
    toScalaObservable[R](rx.Observable.combineLatest[T, U, R](this.asJavaObservable, that.asJavaObservable, selector))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.png" alt="" />
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   *
   * @return An [[rx.lang.scala.Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleWithTimeout(timeout.length, timeout.unit))
  }

  /**
   * Return an Observable that mirrors the source Observable, except that it drops items emitted by the source
   * Observable that are followed by another item within a computed debounce duration.
   *
   * <img width="640" height="425" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.f.png" alt="" />
   *
   * @param debounceSelector function to retrieve a sequence that indicates the throttle duration for each item
   * @return an Observable that omits items emitted by the source Observable that are followed by another item
   *         within a computed debounce duration
   */
  def debounce(debounceSelector: T => Observable[Any]): Observable[T] = {
    val fJava = new rx.functions.Func1[T, rx.Observable[Any]] {
      override def call(t: T) = debounceSelector(t).asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    toScalaObservable[T](asJavaObservable.debounce[Any](fJava))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.png" alt="" />
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   *
   * @return An [[rx.lang.scala.Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.debounce(timeout.length, timeout.unit))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.png" alt="" />
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.debounce(timeout.length, timeout.unit, scheduler))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.png" alt="" />
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleWithTimeout(timeout.length, timeout.unit, scheduler))
  }

  /**
   * Throttles by skipping value until `skipDuration` passes and then emits the next received value.
   *
   * This differs from `Observable.throttleLast` in that this only tracks passage of time whereas `Observable.throttleLast` ticks at scheduled intervals.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.png" alt="" />
   *
   * @param skipDuration
   *            Time to wait before sending another value after emitting last value.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   */
  def throttleFirst(skipDuration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleFirst(skipDuration.length, skipDuration.unit, scheduler))
  }

  /**
   * Throttles by skipping value until `skipDuration` passes and then emits the next received value.
   *
   * This differs from `Observable.throttleLast` in that this only tracks passage of time whereas `Observable.throttleLast` ticks at scheduled intervals.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.png" alt="" />
   *
   * @param skipDuration
   *            Time to wait before sending another value after emitting last value.
   * @return Observable which performs the throttle operation.
   */
  def throttleFirst(skipDuration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleFirst(skipDuration.length, skipDuration.unit))
  }

  /**
   * Throttles by returning the last value of each interval defined by 'intervalDuration'.
   *
   * This differs from `Observable.throttleFirst` in that this ticks along at a scheduled interval whereas `Observable.throttleFirst` does not tick, it just tracks passage of time.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.png" alt="" />
   *
   * @param intervalDuration
   *            Duration of windows within with the last value will be chosen.
   * @return Observable which performs the throttle operation.
   */
  def throttleLast(intervalDuration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleLast(intervalDuration.length, intervalDuration.unit))
  }

  /**
   * Throttles by returning the last value of each interval defined by 'intervalDuration'.
   *
   * This differs from `Observable.throttleFirst` in that this ticks along at a scheduled interval whereas `Observable.throttleFirst` does not tick, it just tracks passage of time.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.png" alt="" />
   *
   * @param intervalDuration
   *            Duration of windows within with the last value will be chosen.
   * @return Observable which performs the throttle operation.
   */
  def throttleLast(intervalDuration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleLast(intervalDuration.length, intervalDuration.unit, scheduler))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, observers are notified of a `TimeoutException`.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1.png" alt="" />
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @return the source Observable modified to notify observers of a
   *         `TimeoutException` in case of a timeout
   */
  def timeout(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(timeout.length, timeout.unit))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, a specified fallback Observable produces future items and
   * notifications from that point on.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2.png" alt="" />
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param other fallback Observable to use in case of a timeout
   * @return the source Observable modified to switch to the fallback
   *         Observable in case of a timeout
   */
  def timeout[U >: T](timeout: Duration, other: Observable[U]): Observable[U] = {
    val otherJava: rx.Observable[_ <: U] = other.asJavaObservable
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(timeout.length, timeout.unit, otherJava))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, the observer is notified of a `TimeoutException`.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1s.png" alt="" />
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param scheduler Scheduler to run the timeout timers on
   * @return the source Observable modified to notify observers of a
   *         `TimeoutException` in case of a timeout
   */
  def timeout(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(timeout.length, timeout.unit, scheduler.asJavaScheduler))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, a specified fallback Observable sequence produces future
   * items and notifications from that point on.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2s.png" alt="" />
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param other Observable to use as the fallback in case of a timeout
   * @param scheduler Scheduler to run the timeout timers on
   * @return the source Observable modified so that it will switch to the
   *         fallback Observable in case of a timeout
   */
  def timeout[U >: T](timeout: Duration, other: Observable[U], scheduler: Scheduler): Observable[U] = {
    val otherJava: rx.Observable[_ <: U] = other.asJavaObservable
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(timeout.length, timeout.unit, otherJava, scheduler.asJavaScheduler))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but emits a TimeoutException if an item emitted by
   * the source Observable doesn't arrive within a window of time after the emission of the
   * previous item, where that period of time is measured by an Observable that is a function
   * of the previous item.
   * <p>
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout3.png" alt="" />
   * </p>
   * Note: The arrival of the first source item is never timed out.
   *
   * @param timeoutSelector
   *            a function that returns an observable for each item emitted by the source
   *            Observable and that determines the timeout window for the subsequent item
   * @return an Observable that mirrors the source Observable, but emits a TimeoutException if a item emitted by
   *         the source Observable takes longer to arrive than the time window defined by the
   *         selector for the previously emitted item
   */
  def timeout(timeoutSelector: T => Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout({ t: T => timeoutSelector(t).asJavaObservable.asInstanceOf[rx.Observable[Any]] }))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but that switches to a fallback
   * Observable if an item emitted by the source Observable doesn't arrive within a window of time
   * after the emission of the previous item, where that period of time is measured by an
   * Observable that is a function of the previous item.
   * <p>
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout4.png" alt="" />
   * </p>
   * Note: The arrival of the first source item is never timed out.
   * 
   * @param timeoutSelector
   *            a function that returns an observable for each item emitted by the source
   *            Observable and that determines the timeout window for the subsequent item
   * @param other
   *            the fallback Observable to switch to if the source Observable times out
   * @return an Observable that mirrors the source Observable, but switches to mirroring a
   *         fallback Observable if a item emitted by the source Observable takes longer to arrive
   *         than the time window defined by the selector for the previously emitted item
   */
  def timeout[U >: T](timeoutSelector: T => Observable[Any], other: Observable[U]): Observable[U] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(
      { t: U => timeoutSelector(t.asInstanceOf[T]).asJavaObservable.asInstanceOf[rx.Observable[Any]] },
      other.asJavaObservable))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but emits a TimeoutException
   * if either the first item emitted by the source Observable or any subsequent item
   * don't arrive within time windows defined by other Observables.
   * <p>
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout5.png" alt="" />
   * </p>
   * @param firstTimeoutSelector
   *            a function that returns an Observable that determines the timeout window for the
   *            first source item
   * @param timeoutSelector
   *            a function that returns an Observable for each item emitted by the source
   *            Observable and that determines the timeout window in which the subsequent source
   *            item must arrive in order to continue the sequence
   * @return an Observable that mirrors the source Observable, but emits a TimeoutException if either the first item or any subsequent item doesn't
   *         arrive within the time windows specified by the timeout selectors
   */
  def timeout(firstTimeoutSelector: () => Observable[Any], timeoutSelector: T => Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(
      { firstTimeoutSelector().asJavaObservable.asInstanceOf[rx.Observable[Any]] },
      { t: T => timeoutSelector(t).asJavaObservable.asInstanceOf[rx.Observable[Any]] }))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but switches to a fallback
   * Observable if either the first item emitted by the source Observable or any subsequent item
   * don't arrive within time windows defined by other Observables.
   * <p>
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout6.png" alt="" />
   * </p>
   * @param firstTimeoutSelector
   *            a function that returns an Observable which determines the timeout window for the
   *            first source item
   * @param timeoutSelector
   *            a function that returns an Observable for each item emitted by the source
   *            Observable and that determines the timeout window in which the subsequent source
   *            item must arrive in order to continue the sequence
   * @param other
   *            the fallback Observable to switch to if the source Observable times out
   * @return an Observable that mirrors the source Observable, but switches to the `other` Observable if either the first item emitted by the source Observable or any
   *         subsequent item don't arrive within time windows defined by the timeout selectors
   */
  def timeout[U >: T](firstTimeoutSelector: () => Observable[Any], timeoutSelector: T => Observable[Any], other: Observable[U]): Observable[U] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(
      { firstTimeoutSelector().asJavaObservable.asInstanceOf[rx.Observable[Any]] },
      { t: U => timeoutSelector(t.asInstanceOf[T]).asJavaObservable.asInstanceOf[rx.Observable[Any]] },
      other.asJavaObservable))
  }

  /**
   * Returns an Observable that sums up the elements of this Observable.
   *
   * This operation is only available if the elements of this Observable are numbers, otherwise
   * you will get a compilation error.
   *
   * @return an Observable emitting the sum of all the elements of the source Observable
   *         as its single item.
   *
   * @usecase def sum: Observable[T]
   *   @inheritdoc
   */
  def sum[U >: T](implicit num: Numeric[U]): Observable[U] = {
    foldLeft(num.zero)(num.plus)
  }

  /**
   * Returns an Observable that multiplies up the elements of this Observable.
   *
   * This operation is only available if the elements of this Observable are numbers, otherwise
   * you will get a compilation error.
   *
   * @return an Observable emitting the product of all the elements of the source Observable
   *         as its single item.
   *
   * @usecase def product: Observable[T]
   *   @inheritdoc
   */
  def product[U >: T](implicit num: Numeric[U]): Observable[U] = {
    foldLeft(num.one)(num.times)
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or
   * a default value if the source Observable is empty.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrDefault.png" alt="" />
   *
   * @param default
   *            The default value to emit if the source Observable doesn't emit anything.
   *            This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits only the very first item from the source, or a default value
   *         if the source Observable completes without emitting any item.
   */
  def firstOrElse[U >: T](default: => U): Observable[U] = {
    take(1).singleOrElse(default)
  }

  /**
   * Returns an Observable that emits only an `Option` with the very first item emitted by the source Observable,
   * or `None` if the source Observable is empty.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrDefault.png" alt="" />
   *
   * @return an Observable that emits only an `Option` with the very first item from the source, or `None`
   *         if the source Observable completes without emitting any item.
   */
  def headOption: Observable[Option[T]] = {
    take(1).singleOption
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or
   * a default value if the source Observable is empty.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrDefault.png" alt="" />
   *
   * @param default
   *            The default value to emit if the source Observable doesn't emit anything.
   *            This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits only the very first item from the source, or a default value
   *         if the source Observable completes without emitting any item.
   */
  def headOrElse[U >: T](default: => U): Observable[U] = firstOrElse(default)

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or raises an
   * `NoSuchElementException` if the source Observable is empty.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/first.png" alt="" />
   * 
   * @return an Observable that emits only the very first item emitted by the source Observable, or raises an
   *         `NoSuchElementException` if the source Observable is empty
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#wiki-first">RxJava Wiki: first()</a>
   * @see "MSDN: Observable.firstAsync()"
   */
  def first: Observable[T] = {
    toScalaObservable[T](asJavaObservable.first)
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or raises an
   * `NoSuchElementException` if the source Observable is empty.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/first.png" alt="" />
   * 
   * @return an Observable that emits only the very first item emitted by the source Observable, or raises an
   *         `NoSuchElementException` if the source Observable is empty
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#wiki-first">RxJava Wiki: first()</a>
   * @see "MSDN: Observable.firstAsync()"
   * @see [[Observable.first]]
   */
  def head: Observable[T] = first

  /**
   * Returns an Observable that emits all items except the first one, or raises an `UnsupportedOperationException`
   * if the source Observable is empty.
   *
   * @return an Observable that emits all items except the first one, or raises an `UnsupportedOperationException`
   *         if the source Observable is empty.
   */
  def tail: Observable[T] = {
    lift {
      (subscriber: Subscriber[T]) => {
        new Subscriber[T](subscriber) {
          var isFirst = true

          override def onNext(v: T): Unit = {
            if (isFirst) {
              isFirst = false
              request(1)
            }
            else {
              subscriber.onNext(v)
            }
          }

          override def onError(e: Throwable): Unit = subscriber.onError(e)

          override def onCompleted(): Unit = {
            if (isFirst) {
              subscriber.onError(new UnsupportedOperationException("tail of empty Observable"))
            } else {
              subscriber.onCompleted
            }
          }
        }
      }
    }
  }

  /**
   * Returns an Observable that emits the last item emitted by the source Observable or notifies observers of
   * an `NoSuchElementException` if the source Observable is empty.
   * 
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/last.png" alt="" />
   * 
   * @return an Observable that emits the last item from the source Observable or notifies observers of an
   *         error
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Filtering-Observable-Operators#wiki-last">RxJava Wiki: last()</a>
   * @see "MSDN: Observable.lastAsync()"
   */
  def last: Observable[T] = {
    toScalaObservable[T](asJavaObservable.last)
  }

  /**
   * Returns an Observable that emits only an `Option` with the last item emitted by the source Observable,
   * or `None` if the source Observable completes without emitting any items.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrDefault.png" alt="" />
   *
   * @return an Observable that emits only an `Option` with the last item emitted by the source Observable,
   *         or `None` if the source Observable is empty
   */
  def lastOption: Observable[Option[T]] = {
    takeRight(1).singleOption
  }

  /**
   * Returns an Observable that emits only the last item emitted by the source Observable, or a default item
   * if the source Observable completes without emitting any items.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrDefault.png" alt="" />
   *
   * @param default the default item to emit if the source Observable is empty.
   *                This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits only the last item emitted by the source Observable, or a default item
   *         if the source Observable is empty
   */
  def lastOrElse[U >: T](default: => U): Observable[U] = {
    takeRight(1).singleOrElse(default)
  }

  /**
   * If the source Observable completes after emitting a single item, return an Observable that emits that
   * item. If the source Observable emits more than one item or no items, notify of an `IllegalArgumentException`
   * or `NoSuchElementException` respectively.
   * 
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/single.png" alt="" />
   * 
   * @return an Observable that emits the single item emitted by the source Observable
   * @throws java.lang.IllegalArgumentException if the source emits more than one item
   * @throws java.util.NoSuchElementException if the source emits no items
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#wiki-single-and-singleordefault">RxJava Wiki: single()</a>
   * @see "MSDN: Observable.singleAsync()"
   */
  def single: Observable[T] = {
    toScalaObservable[T](asJavaObservable.single)
  }

  /**
   * If the source Observable completes after emitting a single item, return an Observable that emits an `Option`
   * with that item; if the source Observable is empty, return an Observable that emits `None`.
   * If the source Observable emits more than one item, throw an `IllegalArgumentException`.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrDefault.png" alt="" />
   *
   * @return an Observable that emits an `Option` with the single item emitted by the source Observable, or
   *         `None` if the source Observable is empty
   * @throws java.lang.IllegalArgumentException if the source Observable emits more than one item
   */
  def singleOption: Observable[Option[T]] = {
    val jObservableOption = map(Some(_)).asJavaObservable.asInstanceOf[rx.Observable[Option[T]]]
    toScalaObservable[Option[T]](jObservableOption.singleOrDefault(None))
  }

  /**
   * If the source Observable completes after emitting a single item, return an Observable that emits that
   * item; if the source Observable is empty, return an Observable that emits a default item. If the source
   * Observable emits more than one item, throw an `IllegalArgumentException`.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrDefault.png" alt="" />
   *
   * @param default a default value to emit if the source Observable emits no item.
   *                This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits the single item emitted by the source Observable, or a default item if
   *         the source Observable is empty
   * @throws java.lang.IllegalArgumentException if the source Observable emits more than one item
   */
  def singleOrElse[U >: T](default: => U): Observable[U] = {
    singleOption.map {
      case Some(element) => element
      case None => default
    }
  }

  /**
   * Returns an Observable that emits the items emitted by the source Observable or a specified default item
   * if the source Observable is empty.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/defaultIfEmpty.png" alt="" />
   *
   * @param default the item to emit if the source Observable emits no items. This is a by-name parameter, so it is
   *                only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits either the specified default item if the source Observable emits no
   *         items, or the items emitted by the source Observable
   */
  def orElse[U >: T](default: => U): Observable[U] = {
    val jObservableOption = map(Some(_)).asJavaObservable.asInstanceOf[rx.Observable[Option[T]]]
    val o = toScalaObservable[Option[T]](jObservableOption.defaultIfEmpty(None))
    o map {
      case Some(element) => element
      case None => default
    }
  }

  /**
   * Returns an Observable that forwards all sequentially distinct items emitted from the source Observable.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.png" alt="" />
   *
   * @return an Observable of sequentially distinct items
   */
  def distinctUntilChanged: Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinctUntilChanged)
  }

  /**
   * Returns an Observable that forwards all items emitted from the source Observable that are sequentially
   * distinct according to a key selector function.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.key.png" alt="" />
   *
   * @param keySelector
   *            a function that projects an emitted item to a key value which is used for deciding whether an item is sequentially
   *            distinct from another one or not
   * @return an Observable of sequentially distinct items
   */
  def distinctUntilChanged[U](keySelector: T => U): Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinctUntilChanged[U](keySelector))
  }

  /**
   * Returns an Observable that forwards all distinct items emitted from the source Observable.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.png" alt="" />
   *
   * @return an Observable of distinct items
   */
  def distinct: Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinct())
  }

  /**
   * Returns an Observable that forwards all items emitted from the source Observable that are distinct according
   * to a key selector function.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.key.png" alt="" />
   *
   * @param keySelector
   *            a function that projects an emitted item to a key value which is used for deciding whether an item is
   *            distinct from another one or not
   * @return an Observable of distinct items
   */
  def distinct[U](keySelector: T => U): Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinct[U](keySelector))
  }

  /**
   * Returns an Observable that counts the total number of elements in the source Observable.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/count.png" alt="" />
   *
   * @return an Observable emitting the number of counted elements of the source Observable
   *         as its single item.
   */
  def length: Observable[Int] = {
    toScalaObservable[Integer](asJavaObservable.count()).map(_.intValue())
  }

  /**
   * Returns an Observable that counts the total number of elements in the source Observable.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/count.png" alt="" />
   *
   * @return an Observable emitting the number of counted elements of the source Observable
   *         as its single item.
   */
  def size: Observable[Int] = length

  /**
   * Retry subscription to origin Observable upto given retry count.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="" />
   *
   * If [[rx.lang.scala.Observer.onError]] is invoked the source Observable will be re-subscribed to as many times as defined by retryCount.
   *
   * Any [[rx.lang.scala.Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   *
   * @param retryCount
   *            Number of retry attempts before failing.
   * @return Observable with retry logic.
   */
  def retry(retryCount: Long): Observable[T] = {
    toScalaObservable[T](asJavaObservable.retry(retryCount))
  }

  /**
   * Retry subscription to origin Observable whenever onError is called (infinite retry count).
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="" />
   *
   * If [[rx.lang.scala.Observer.onError]] is invoked the source Observable will be re-subscribed to.
   *
   * Any [[rx.lang.scala.Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   * @return Observable with retry logic.
   */
  def retry: Observable[T] = {
    toScalaObservable[T](asJavaObservable.retry())
  }

  /**
   * Returns an Observable that mirrors the source Observable, resubscribing to it if it calls `onError`
   * and the predicate returns true for that specific exception and retry count.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="" />
   *
   * @param predicate the predicate that determines if a resubscription may happen in case of a specific exception and retry count
   * @return the source Observable modified with retry logic
   */
  def retry(predicate: (Int, Throwable) => Boolean): Observable[T] = {
    val f = new Func2[java.lang.Integer, Throwable, java.lang.Boolean] {
      def call(times: java.lang.Integer, e: Throwable): java.lang.Boolean = predicate(times, e)
    }
    toScalaObservable[T](asJavaObservable.retry(f))
  }

  /**
   * Returns an Observable that emits the same values as the source observable with the exception of an
   * `onError`. An `onError` notification from the source will result in the emission of a
   * `Throwable` to the Observable provided as an argument to the `notificationHandler`
   *  function. If the Observable returned `onCompletes` or `onErrors` then `retry` will call
   * `onCompleted` or `onError` on the child subscription. Otherwise, this Observable will
   * resubscribe to the source Observable.
   * <p>
   * <img width="640" height="430" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/retryWhen.f.png" alt="" />
   *
   * Example:
   *
   * This retries 3 times, each time incrementing the number of seconds it waits.
   *
   * @example
   *
   * This retries 3 times, each time incrementing the number of seconds it waits.
   *
   * {{{
   * Observable[String]({ subscriber =>
   *   println("subscribing")
   *   subscriber.onError(new RuntimeException("always fails"))
   * }).retryWhen({ throwableObservable =>
   *   throwableObservable.zipWith(Observable.from(1 to 3))((t, i) => i).flatMap(i => {
   *     println("delay retry by " + i + " second(s)")
   *     Observable.timer(Duration(i, TimeUnit.SECONDS))
   *   })
   * }).toBlocking.foreach(s => println(s))
   * }}}
   *
   * Output is:
   *
   * {{{
   * subscribing
   * delay retry by 1 second(s)
   * subscribing
   * delay retry by 2 second(s)
   * subscribing
   * delay retry by 3 second(s)
   * subscribing
   * }}}
   *
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>`retryWhen` operates by default on the `trampoline` [[Scheduler]].</dd>
   * </dl>
   *
   * @param notificationHandler receives an Observable of a Throwable with which a user can complete or error, aborting the
   *            retry
   * @return the source Observable modified with retry logic
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#retrywhen">RxJava Wiki: retryWhen()</a>
   * @see RxScalaDemo.retryWhenDifferentExceptionsExample for a more intricate example
   * @since 0.20
   */
  def retryWhen(notificationHandler: Observable[Throwable] => Observable[Any]): Observable[T] = {
    val f: Func1[_ >: rx.Observable[_ <: Throwable], _ <: rx.Observable[_ <: Any]] =
      (jOt: rx.Observable[_ <: Throwable]) => {
        val ot = toScalaObservable[Throwable](jOt)
        notificationHandler(ot).asJavaObservable
      }

    toScalaObservable[T](asJavaObservable.retryWhen(f))
  }

  /**
   * Returns an Observable that emits the same values as the source observable with the exception of an `onError`.
   * An onError will emit a `Throwable` to the Observable provided as an argument to the `notificationHandler`
   * function. If the Observable returned `onCompletes` or `onErrors` then retry will call `onCompleted`
   * or `onError` on the child subscription. Otherwise, this observable will resubscribe to the source observable, on a particular Scheduler.
   * <p>
   * <img width="640" height="430" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/retryWhen.f.png" alt="" />
   * <p>
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>you specify which [[Scheduler]] this operator will use</dd>
   * </dl>
   *
   * @param notificationHandler receives an Observable of a Throwable with which a user can complete or error, aborting
   *                            the retry
   * @param scheduler the Scheduler on which to subscribe to the source Observable
   * @return the source Observable modified with retry logic
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#retrywhen">RxJava Wiki: retryWhen()</a>
   * @see RxScalaDemo.retryWhenDifferentExceptionsExample for a more intricate example
   * @since 0.20
   */
  def retryWhen(notificationHandler: Observable[Throwable] => Observable[Any], scheduler: Scheduler): Observable[T] = {
    val f: Func1[_ >: rx.Observable[_ <: Throwable], _ <: rx.Observable[_ <: Any]] =
      (jOt: rx.Observable[_ <: Throwable]) => {
        val ot = toScalaObservable[Throwable](jOt)
        notificationHandler(ot).asJavaObservable
      }

    toScalaObservable[T](asJavaObservable.retryWhen(f, scheduler))
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable indefinitely.
   * <p>
   * <img width="640" height="309" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.o.png" alt="" />
   *
   * @return an Observable that emits the items emitted by the source Observable repeatedly and in sequence
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat: Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat())
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable indefinitely,
   * on a particular Scheduler.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.os.png" alt="" />
   *
   * @param scheduler the Scheduler to emit the items on
   * @return an Observable that emits the items emitted by the source Observable repeatedly and in sequence
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(scheduler))
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable at most `count` times.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.on.png" alt="" />
   *
   * @param count the number of times the source Observable items are repeated,
   *              a count of 0 will yield an empty sequence
   * @return an Observable that repeats the sequence of items emitted by the source Observable at most `count` times
   * @throws java.lang.IllegalArgumentException if `count` is less than zero
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(count: Long): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(count))
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable
   * at most `count` times, on a particular Scheduler.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.ons.png" alt="" />
   *
   * @param count the number of times the source Observable items are repeated,
   *              a count of 0 will yield an empty sequence
   * @param scheduler the `Scheduler` to emit the items on
   * @return an Observable that repeats the sequence of items emitted by the source Observable at most `count` times
   *         on a particular Scheduler
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(count: Long, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(count, scheduler))
  }

  /**
   * Returns an Observable that emits the same values as the source Observable with the exception of an
   * `onCompleted`. An `onCompleted` notification from the source will result in the emission of
   * a `scala.Unit` to the Observable provided as an argument to the `notificationHandler`
   * function. If the Observable returned `onCompletes` or `onErrors` then `repeatWhen` will
   * call `onCompleted` or `onError` on the child subscription. Otherwise, this Observable will
   * resubscribe to the source Observable, on a particular Scheduler.
   * <p>
   * <img width="640" height="430" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatWhen.f.png" alt="" />
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>you specify which [[Scheduler]] this operator will use</dd>
   * </dl>
   *
   * @param notificationHandler receives an Observable of a Unit with which a user can complete or error, aborting the repeat.
   * @param scheduler the Scheduler to emit the items on
   * @return the source Observable modified with repeat logic
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#repeatwhen">RxJava Wiki: repeatWhen()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   * @since 0.20
   */
  def repeatWhen(notificationHandler: Observable[Unit] => Observable[Any], scheduler: Scheduler): Observable[T] = {
    val f: Func1[_ >: rx.Observable[_ <: Void], _ <: rx.Observable[_ <: Any]] =
      (jOv: rx.Observable[_ <: Void]) => {
        val ov = toScalaObservable[Void](jOv)
        notificationHandler(ov.map( _ => Unit )).asJavaObservable
      }

    toScalaObservable[T](asJavaObservable.repeatWhen(f, scheduler))
  }

  /**
   * Returns an Observable that emits the same values as the source Observable with the exception of an
   * `onCompleted`. An `onCompleted` notification from the source will result in the emission of
   * a `scala.Unit` to the Observable provided as an argument to the `notificationHandler`
   * function. If the Observable returned `onCompletes` or `onErrors` then `repeatWhen` will
   * call `onCompleted` or `onError` on the child subscription. Otherwise, this Observable will
   * resubscribe to the source observable.
   * <p>
   * <img width="640" height="430" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatWhen.f.png" alt="" />
   *
   * @example
   *
   * This repeats 3 times, each time incrementing the number of seconds it waits.
   *
   * {{{
   * Observable[String]({ subscriber =>
   *   println("subscribing")
   *   subscriber.onCompleted()
   * }).repeatWhen({ unitObservable =>
   *   unitObservable.zipWith(Observable.from(1 to 3))((u, i) => i).flatMap(i => {
   *     println("delay repeat by " + i + " second(s)")
   *     Observable.timer(Duration(i, TimeUnit.SECONDS))
   *   })
   * }).toBlocking.foreach(s => println(s))
   * }}}
   *
   * Output is:
   *
   * {{{
   * subscribing
   * delay repeat by 1 second(s)
   * subscribing
   * delay repeat by 2 second(s)
   * subscribing
   * delay repeat by 3 second(s)
   * subscribing
   * }}}
   *
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>`repeatWhen` operates by default on the `trampoline` [[Scheduler]].</dd>
   * </dl>
   *
   * @param notificationHandler receives an Observable of a Unit with which a user can complete or error, aborting the repeat.
   * @return the source Observable modified with repeat logic
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#repeatwhen">RxJava Wiki: repeatWhen()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   * @since 0.20
   */
  def repeatWhen(notificationHandler: Observable[Unit] => Observable[Any]): Observable[T] = {
    val f: Func1[_ >: rx.Observable[_ <: Void], _ <: rx.Observable[_ <: Any]] =
      (jOv: rx.Observable[_ <: Void]) => {
        val ov = toScalaObservable[Void](jOv)
        notificationHandler(ov.map( _ => Unit )).asJavaObservable
      }

    toScalaObservable[T](asJavaObservable.repeatWhen(f))
  }

  /**
   * Converts an Observable into a [[rx.lang.scala.observables.BlockingObservable BlockingObservable]] (an Observable with blocking
   * operators).
   *
   * @return a [[rx.lang.scala.observables.BlockingObservable BlockingObservable]] version of this Observable
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators">Blocking Observable Operators</a>
   * @since 0.19
   */
  def toBlocking: BlockingObservable[T] = {
    new BlockingObservable[T](this)
  }

  /** Tests whether a predicate holds for some of the elements of this `Observable`.
    *
    *  @param   p     the predicate used to test elements.
    *  @return        an Observable emitting one single Boolean, which is `true` if the given predicate `p`
    *                 holds for some of the elements of this Observable, and `false` otherwise.
    */
  def exists(p: T => Boolean): Observable[Boolean] = {
    toScalaObservable[java.lang.Boolean](asJavaObservable.exists(p)).map(_.booleanValue())
  }

  /** Tests whether this `Observable` emits no elements.
    *
    *  @return        an Observable emitting one single Boolean, which is `true` if this `Observable`
    *                 emits no elements, and `false` otherwise.
    */
  def isEmpty: Observable[Boolean] = {
    toScalaObservable[java.lang.Boolean](asJavaObservable.isEmpty()).map(_.booleanValue())
  }

  def withFilter(p: T => Boolean): WithFilter[T] = {
    new WithFilter[T](p, asJavaObservable)
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param observer the observer
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(observer: Observer[T]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(observer.asJavaObserver))
  }

  /**
   * Invokes an action when the source Observable calls <code>onNext</code>.
   *
   * @param onNext the action to invoke when the source Observable calls <code>onNext</code>
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnNext(onNext: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnNext(onNext))
  }

  /**
   * Invokes an action if the source Observable calls `onError`.
   *
   * @param onError the action to invoke if the source Observable calls
   *                `onError`
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnError(onError: Throwable => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnError(onError))
  }

  /**
   * Invokes an action when the source Observable calls `onCompleted`.
   *
   * @param onCompleted the action to invoke when the source Observable calls
   *                    `onCompleted`
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnCompleted(onCompleted: => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnCompleted(() => onCompleted))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnNext(onNext))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   * @param onError this function will be called if an error occurs
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit, onError: Throwable => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(Observer(onNext, onError, ()=>{})))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   * @param onError this function will be called if an error occurs
   * @param onCompleted the action to invoke when the source Observable calls
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(Observer(onNext, onError,onCompleted)))
  }

  /**
   * Modifies the source `Observable` so that it invokes the given action when it is subscribed from
   * its subscribers. Each subscription will result in an invocation of the given action except when the
   * source `Observable` is reference counted, in which case the source `Observable` will invoke
   * the given action for the first subscription.
   * <p>
   * <img width="640" height="390" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnSubscribe.png" alt="" />
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>`onSubscribe` does not operate by default on a particular `Scheduler`.</dd>
   * </dl>
   *
   * @param onSubscribe
     *            the action that gets called when an observer subscribes to this `Observable`
   * @return the source `Observable` modified so as to call this Action when appropriate
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#doonsubscribe">RxJava wiki: doOnSubscribe</a>
   * @since 0.20
   */
  def doOnSubscribe(onSubscribe: => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnSubscribe(() => onSubscribe))
  }

  /**
   * Modifies an Observable so that it invokes an action when it calls `onCompleted` or `onError`.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnTerminate.png" alt="" />
   * <p>
   * This differs from `finallyDo` in that this happens **before** `onCompleted/onError` are emitted.
   *
   * @param onTerminate the action to invoke when the source Observable calls `onCompleted` or `onError`
   * @return the source Observable with the side-effecting behavior applied
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#wiki-doonterminate">RxJava Wiki: doOnTerminate()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229804.aspx">MSDN: Observable.Do</a>
   */
  def doOnTerminate(onTerminate: => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnTerminate(() => onTerminate))
  }

  /**
   * Modifies the source `Observable` so that it invokes the given action when it is unsubscribed from
   * its subscribers. Each un-subscription will result in an invocation of the given action except when the
   * source `Observable` is reference counted, in which case the source `Observable` will invoke
   * the given action for the very last un-subscription.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnUnsubscribe.png" alt="" />
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>`doOnUnsubscribe` does not operate by default on a particular `Scheduler`.</dd>
   * </dl>
   *
   * @param onUnsubscribe
     *            the action that gets called when this `Observable` is unsubscribed
   * @return the source `Observable` modified so as to call this Action when appropriate
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#doonunsubscribe">RxJava wiki: doOnUnsubscribe</a>
   * @since 0.20
   */
  def doOnUnsubscribe(onUnsubscribe: => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnUnsubscribe(() => onUnsubscribe))
  }

  /**
   * Given two Observables, mirror the one that first emits an item.
   *
   * <img width="640" height="385" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/amb.png" alt="" />
   *
   * @param that
   *            an Observable competing to react first
   * @return an Observable that emits the same sequence of items as whichever of `this` or `that` first emitted an item.
   */
  def amb[U >: T](that: Observable[U]): Observable[U] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable[U](rx.Observable.amb(thisJava, thatJava))
  }

  /**
   * Returns an Observable that emits the items emitted by the source Observable shifted forward in time by a
   * specified delay. Error notifications from the source Observable are not delayed.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.png" alt="" />
   * 
   * @param delay the delay to shift the source by
   * @return the source Observable shifted in time by the specified delay
   */
  def delay(delay: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delay(delay.length, delay.unit))
  }

  /**
   * Returns an Observable that emits the items emitted by the source Observable shifted forward in time by a
   * specified delay. Error notifications from the source Observable are not delayed.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.png" alt="" />
   * 
   * @param delay the delay to shift the source by
   * @param scheduler the Scheduler to use for delaying
   * @return the source Observable shifted in time by the specified delay
   */
  def delay(delay: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delay(delay.length, delay.unit, scheduler))
  }

  /**
   * Returns an Observable that delays the emissions of the source Observable via another Observable on a
   * per-item basis.
   * <p>
   * <img width="640" height="450" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.o.png" alt="" />
   * <p>
   * Note: the resulting Observable will immediately propagate any `onError` notification
   * from the source Observable.
   *
   * @param itemDelay a function that returns an Observable for each item emitted by the source Observable, which is
   *                  then used to delay the emission of that item by the resulting Observable until the Observable
   *                  returned from `itemDelay` emits an item
   * @return an Observable that delays the emissions of the source Observable via another Observable on a per-item basis
   */
  def delay(itemDelay: T => Observable[Any]): Observable[T] = {
    val itemDelayJava = new Func1[T, rx.Observable[Any]] {
      override def call(t: T): rx.Observable[Any] =
        itemDelay(t).asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    toScalaObservable[T](asJavaObservable.delay[Any](itemDelayJava))
  }

  /**
   * Returns an Observable that delays the subscription to and emissions from the souce Observable via another
   * Observable on a per-item basis.
   * <p>
   * <img width="640" height="450" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.oo.png" alt="" />
   * <p>
   * Note: the resulting Observable will immediately propagate any `onError` notification
   * from the source Observable.
   *
   * @param subscriptionDelay a function that returns an Observable that triggers the subscription to the source Observable
   *                          once it emits any item
   * @param itemDelay a function that returns an Observable for each item emitted by the source Observable, which is
   *                  then used to delay the emission of that item by the resulting Observable until the Observable
   *                  returned from `itemDelay` emits an item
   * @return an Observable that delays the subscription and emissions of the source Observable via another
   *         Observable on a per-item basis
   */
  def delay(subscriptionDelay: () => Observable[Any], itemDelay: T => Observable[Any]): Observable[T] = {
    val subscriptionDelayJava = new Func0[rx.Observable[Any]] {
      override def call(): rx.Observable[Any] =
        subscriptionDelay().asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    val itemDelayJava = new Func1[T, rx.Observable[Any]] {
      override def call(t: T): rx.Observable[Any] =
        itemDelay(t).asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    toScalaObservable[T](asJavaObservable.delay[Any, Any](subscriptionDelayJava, itemDelayJava))
  }

  /**
   * Return an Observable that delays the subscription to the source Observable by a given amount of time.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.png" alt="" />
   * 
   * @param delay the time to delay the subscription
   * @return an Observable that delays the subscription to the source Observable by the given amount
   */
  def delaySubscription(delay: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delaySubscription(delay.length, delay.unit))
  }

  /**
   * Return an Observable that delays the subscription to the source Observable by a given amount of time,
   * both waiting and subscribing on a given Scheduler.
   *
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.s.png" alt="" />
   * 
   * @param delay the time to delay the subscription
   * @param scheduler the Scheduler on which the waiting and subscription will happen
   * @return an Observable that delays the subscription to the source Observable by a given
   *         amount, waiting and subscribing on the given Scheduler
   */
  def delaySubscription(delay: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delaySubscription(delay.length, delay.unit, scheduler))
  }

  /**
   * Returns an Observable that delays the subscription to the source Observable until a second Observable
   * emits an item.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.o.png" alt="" />
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>This version of `delay` operates by default on the `computation` `Scheduler`.</dd>
   * </dl>
   *
   * @param subscriptionDelay
   *            a function that returns an Observable that triggers the subscription to the source Observable
   *            once it emits any item
   * @return an Observable that delays the subscription to the source Observable until the Observable returned
   *         by `subscriptionDelay` emits an item
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#delaysubscription">RxJava wiki: delaySubscription</a>
   */
  def delaySubscription(subscriptionDelay: () => Observable[Any]): Observable[T] = {
    val subscriptionDelayJava = new Func0[rx.Observable[Any]] {
      override def call(): rx.Observable[Any] =
        subscriptionDelay().asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }

    toScalaObservable[T](asJavaObservable.delaySubscription(subscriptionDelayJava))
  }

  /**
   * Returns an Observable that emits the single item at a specified index in a sequence of emissions from a
   * source Observbable.
   * 
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAt.png" alt="" />
   * 
   * @param index
   *            the zero-based index of the item to retrieve
   * @return an Observable that emits a single item: the item at the specified position in the sequence of
   *         those emitted by the source Observable
   * @throws java.lang.IndexOutOfBoundsException
   *             if index is greater than or equal to the number of items emitted by the source
   *             Observable, or index is less than 0
   */
  def elementAt(index: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.elementAt(index))
  }

  /**
   * Returns an Observable that emits the item found at a specified index in a sequence of emissions from a
   * source Observable, or a default item if that index is out of range.
   * 
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAtOrDefault.png" alt="" />
   * 
   * @param index
   *            the zero-based index of the item to retrieve
   * @param default
   *            the default item
   * @return an Observable that emits the item at the specified position in the sequence emitted by the source
   *         Observable, or the default item if that index is outside the bounds of the source sequence
   * @throws java.lang.IndexOutOfBoundsException
   *             if `index` is less than 0
   */
  def elementAtOrDefault[U >: T](index: Int, default: U): Observable[U] = {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.elementAtOrDefault(index, default))
  }

  /**
   * Return an Observable that emits a single `Map` containing values corresponding to items emitted by the
   * source Observable, mapped by the keys returned by a specified `keySelector` function.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.png" alt="" />
   * <p>
   *
   * @param keySelector the function that extracts the key from a source item to be used in the `Map`
   * @param valueSelector the function that extracts the value from a source item to be used in the `Map`
   * @param cbf `CanBuildFrom` to build the `Map`
   * @return an Observable that emits a single item: a `Map` containing the mapped items from the source
   *         Observable
   */
  def to[M[_, _], K, V](keySelector: T => K, valueSelector: T => V)(implicit cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]): Observable[M[K, V]] = {
    lift {
      (subscriber: Subscriber[M[K, V]]) => {
        new Subscriber[T](subscriber) {
          val b = cbf()

          override def onStart(): Unit = request(Long.MaxValue)

          override def onNext(t: T): Unit = {
            val key = keySelector(t)
            val value = valueSelector(t)
            b += key -> value
          }

          override def onError(e: Throwable): Unit = {
            subscriber.onError(e)
          }

          override def onCompleted(): Unit = {
            subscriber.onNext(b.result)
            subscriber.onCompleted()
          }
        }
      }
    }
  }

  /**
   * Return an Observable that emits a single `Map` containing all pairs emitted by the source Observable.
   * This method is unavailable unless the elements are members of `(K, V)`. Each `(K, V)` becomes a key-value
   * pair in the map. If more than one pairs have the same key, the `Map` will contain the latest of
   * those items.
   *
   * @return an Observable that emits a single item: an `Map` containing all pairs from the source Observable
   */
  def toMap[K, V](implicit ev: Observable[T] <:< Observable[(K, V)]): Observable[Map[K, V]] = {
    val o: Observable[(K, V)] = this
    o.toMap(_._1, _._2)
  }

  /**
   * Return an Observable that emits a single `Map` containing all items emitted by the source Observable,
   * mapped by the keys returned by a specified `keySelector` function.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.png" alt="" />
   * <p>
   * If more than one source item maps to the same key, the `Map` will contain the latest of those items.
   *
   * @param keySelector the function that extracts the key from a source item to be used in the `Map`
   * @return an Observable that emits a single item: an `Map` containing the mapped items from the source
   *         Observable
   */
  def toMap[K](keySelector: T => K): Observable[Map[K, T]] = {
    to[Map, K, T](keySelector, v => v)
  }

  /**
   * Return an Observable that emits a single `Map` containing values corresponding to items emitted by the
   * source Observable, mapped by the keys returned by a specified `keySelector` function.
   * <p>
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.png" alt="" />
   * <p>
   * If more than one source item maps to the same key, the `Map` will contain a single entry that
   * corresponds to the latest of those items.
   *
   * @param keySelector the function that extracts the key from a source item to be used in the `Map`
   * @param valueSelector the function that extracts the value from a source item to be used in the `Map`
   * @return an Observable that emits a single item: an `Map` containing the mapped items from the source
   *         Observable
   */
  def toMap[K, V](keySelector: T => K, valueSelector: T => V): Observable[Map[K, V]] = {
    to[Map, K, V](keySelector, valueSelector)
  }

  /**
   * Returns an Observable that emits a Boolean value that indicates whether `this` and `that` Observable sequences are the
   * same by comparing the items emitted by each Observable pairwise.
   * <p>
   * <img width="640" height="385" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.png" alt="" />
   *
   * Note: this method uses `==` to compare elements. It's a bit different from RxJava which uses `Object.equals`.
   *
   * @param that the Observable to compare
   * @return an Observable that emits a `Boolean` value that indicates whether the two sequences are the same
   */
  def sequenceEqual[U >: T](that: Observable[U]): Observable[Boolean] = {
    sequenceEqualWith(that)(_ == _)
  }

  /**
   * Returns an Observable that emits a Boolean value that indicates whether `this` and `that` Observable sequences are the
   * same by comparing the items emitted by each Observable pairwise based on the results of a specified `equality` function.
   * <p>
   * <img width="640" height="385" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.png" alt="" />
   *
   * @param that the Observable to compare
   * @param equality a function used to compare items emitted by each Observable
   * @return an Observable that emits a `Boolean` value that indicates whether the two sequences are the same based on the `equality` function.
   */
  def sequenceEqualWith[U >: T](that: Observable[U])(equality: (U, U) => Boolean): Observable[Boolean] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    val equalityJava: Func2[_ >: U, _ >: U, java.lang.Boolean] = equality
    toScalaObservable[java.lang.Boolean](rx.Observable.sequenceEqual[U](thisJava, thatJava, equalityJava)).map(_.booleanValue)
  }

  /**
   * Returns an Observable that emits records of the time interval between consecutive items emitted by the
   * source Observable.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.png" alt="" />
   *
   * @return an Observable that emits time interval information items
   */
  def timeInterval: Observable[(Duration, T)] = {
    toScalaObservable(asJavaObservable.timeInterval())
      .map(inv => (Duration(inv.getIntervalInMilliseconds, MILLISECONDS), inv.getValue))
  }

  /**
   * Returns an Observable that emits records of the time interval between consecutive items emitted by the
   * source Observable, where this interval is computed on a specified Scheduler.
   * <p>
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.s.png" alt="" />
   *
   * @param scheduler the [[Scheduler]] used to compute time intervals
   * @return an Observable that emits time interval information items
   */
  def timeInterval(scheduler: Scheduler): Observable[(Duration, T)] = {
    toScalaObservable(asJavaObservable.timeInterval(scheduler.asJavaScheduler))
      .map(inv => (Duration(inv.getIntervalInMilliseconds, MILLISECONDS), inv.getValue))
  }

  /**
   * Lifts a function to the current Observable and returns a new Observable that when subscribed to will pass
   * the values of the current Observable through the Operator function.
   * <p>
   * In other words, this allows chaining Observers together on an Observable for acting on the values within
   * the Observable.
   * {{{
   * observable.map(...).filter(...).take(5).lift(new OperatorA()).lift(new OperatorB(...)).subscribe()
   * }}}
   * If the operator you are creating is designed to act on the individual items emitted by a source
   * Observable, use `lift`. If your operator is designed to transform the source Observable as a whole
   * (for instance, by applying a particular set of existing RxJava operators to it) use `#compose`.
   * <dl>
   * <dt><b>Scheduler:</b></dt>
   * <dd>`lift` does not operate by default on a particular [[Scheduler]].</dd>
   * </dl>
   *
   * @param operator the Operator that implements the Observable-operating function to be applied to the source
   *             Observable
   * @return an Observable that is the result of applying the lifted Operator to the source Observable
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
   * @since 0.17
   */
  def lift[R](operator: Subscriber[R] => Subscriber[T]): Observable[R] = {
    toScalaObservable(asJavaObservable.lift(toJavaOperator[T, R](operator)))
  }

  /**
   * Converts the source `Observable[T]` into an `Observable[Observable[T]]` that emits the source Observable as its single emission.
   *
   * <img width="640" height="350" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/nest.png" alt="" />
   *
   * @return an Observable that emits a single item: the source Observable
   */
  def nest: Observable[Observable[T]] = {
    toScalaObservable(asJavaObservable.nest).map(toScalaObservable[T](_))
  }

  /**
   * Subscribes to the [[Observable]] and receives notifications for each element.
   *
   * Alias to `subscribe(T => Unit)`.
   *
   * $noDefaultScheduler
   *
   * @param onNext function to execute for each item.
   * @throws java.lang.IllegalArgumentException if `onNext` is null
   * @throws rx.exceptions.OnErrorNotImplementedException if the [[Observable]] tries to call `onError`
   * @since 0.19
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def foreach(onNext: T => Unit): Unit = {
    asJavaObservable.subscribe(onNext)
  }

  /**
   * Subscribes to the [[Observable]] and receives notifications for each element and error events.
   *
   * Alias to `subscribe(T => Unit, Throwable => Unit)`.
   *
   * $noDefaultScheduler
   *
   * @param onNext function to execute for each item.
   * @param onError function to execute when an error is emitted.
   * @throws java.lang.IllegalArgumentException if `onNext` is null, or if `onError` is null
   * @since 0.19
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def foreach(onNext: T => Unit, onError: Throwable => Unit): Unit = {
    asJavaObservable.subscribe(onNext, onError)
  }

  /**
   * Subscribes to the [[Observable]] and receives notifications for each element and the terminal events.
   *
   * Alias to `subscribe(T => Unit, Throwable => Unit, () => Unit)`.
   *
   * $noDefaultScheduler
   *
   * @param onNext function to execute for each item.
   * @param onError function to execute when an error is emitted.
   * @param onComplete function to execute when completion is signalled.
   * @throws java.lang.IllegalArgumentException if `onNext` is null, or if `onError` is null, or if `onComplete` is null
   * @since 0.19
   * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
   */
  def foreach(onNext: T => Unit, onError: Throwable => Unit, onComplete: () => Unit): Unit = {
    asJavaObservable.subscribe(onNext, onError, onComplete)
  }

  /**
   * Returns an Observable that counts the total number of items emitted by the source Observable and emits
   * this count as a 64-bit Long.
   * <p>
   * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/longCount.png" alt="" />
   * <dl>
   * <dt><b>Backpressure Support:</b></dt>
   * <dd>This operator does not support backpressure because by intent it will receive all values and reduce
   * them to a single `onNext`.</dd>
   * <dt><b>Scheduler:</b></dt>
   * <dd>`countLong` does not operate by default on a particular `Scheduler`.</dd>
   * </dl>
   *
   * @return an Observable that emits a single item: the number of items emitted by the source Observable as a
   *         64-bit Long item
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#count-and-countlong">RxJava wiki: countLong</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229120.aspx">MSDN: Observable.LongCount</a>
   * @see #count()
   */
  def countLong: Observable[Long] = {
    toScalaObservable[java.lang.Long](asJavaObservable.countLong()).map(_.longValue())
  }

  /**
   * Returns an Observable that emits a single `mutable.MultiMap` that contains items emitted by the
   * source Observable keyed by a specified `keySelector` function. The items having the same
   * key will be put into a `Set`.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.png" alt="" />
   *
   * @param keySelector the function that extracts the key from the source items to be used as key in the `mutable.MultiMap`
   * @return an Observable that emits a single item: a `mutable.MultiMap` that contains items emitted by the
   *         source Observable keyed by a specified `keySelector` function.
   */
  def toMultiMap[K, V >: T](keySelector: T => K): Observable[mutable.MultiMap[K, V]] = {
    toMultiMap(keySelector, k => k)
  }

  /**
   * Returns an Observable that emits a single `mutable.MultiMap` that contains values extracted by a
   * specified `valueSelector` function from items emitted by the source Observable, keyed by a
   * specified `keySelector` function. The values having the same key will be put into a `Set`.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.png" alt="" />
   *
   * @param keySelector the function that extracts a key from the source items to be used as key in the `mutable.MultiMap`
   * @param valueSelector the function that extracts a value from the source items to be used as value in the `mutable.MultiMap`
   * @return an Observable that emits a single item: a `mutable.MultiMap` that contains keys and values mapped from
   *         the source Observable
   */
  def toMultiMap[K, V](keySelector: T => K, valueSelector: T => V): Observable[mutable.MultiMap[K, V]] = {
    toMultiMap(keySelector, valueSelector, new mutable.HashMap[K, mutable.Set[V]] with mutable.MultiMap[K, V])
  }

  /**
   * Returns an Observable that emits a single `mutable.MultiMap`, returned by a specified `multiMapFactory` function, that
   * contains values extracted by a specified `valueSelector` function from items emitted by the source Observable, and
   * keyed by the `keySelector` function. The values having the same key will be put into a `Set`.
   *
   * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.png" alt="" />
   *
   * @param keySelector the function that extracts a key from the source items to be used as the key in the `mutable.MultiMap`
   * @param valueSelector the function that extracts a value from the source items to be used as the value in the `mutable.MultiMap`
   * @param multiMapFactory a `mutable.MultiMap` instance to be used. Note: tis is a by-name parameter.
   * @return an Observable that emits a single item: a `mutable.MultiMap` that contains keys and values mapped from the source Observable.
   */
  def toMultiMap[K, V, M <: mutable.MultiMap[K, V]](keySelector: T => K, valueSelector: T => V, multiMapFactory: => M): Observable[M] = {
    lift {
      (subscriber: Subscriber[M]) => {
        new Subscriber[T](subscriber) {
          val mm = multiMapFactory

          override def onStart(): Unit = request(Long.MaxValue)

          override def onNext(t: T): Unit = {
            val key = keySelector(t)
            val value = valueSelector(t)
            mm.addBinding(key, value)
          }

          override def onError(e: Throwable): Unit = {
            subscriber.onError(e)
          }

          override def onCompleted(): Unit = {
            subscriber.onNext(mm)
            subscriber.onCompleted()
          }
        }
      }
    }
  }

  /**
   * Returns an Observable that emits a single item, a collection composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @tparam Col the collection type to build.
   * @return an Observable that emits a single item, a collection containing all of the items emitted by
   *         the source Observable.
   */
  def to[Col[_]](implicit cbf: CanBuildFrom[Nothing, T, Col[T @uncheckedVariance]]): Observable[Col[T @uncheckedVariance]] = {
    lift {
      (subscriber: Subscriber[Col[T]]) => {
        new Subscriber[T](subscriber) {
          val b = cbf()

          override def onStart(): Unit = request(Long.MaxValue)

          override def onNext(t: T): Unit = b += t

          override def onError(e: Throwable): Unit = subscriber.onError(e)

          override def onCompleted(): Unit = {
            subscriber.onNext(b.result)
            subscriber.onCompleted()
          }
        }
      }
    }
  }

  /**
   * Returns an Observable that emits a single item, a `Traversable` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `Traversable` containing all of the items emitted by
   *         the source Observable.
   */
  def toTraversable: Observable[Traversable[T]] = to[Traversable]

  /**
   * Returns an Observable that emits a single item, a `List` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `List` containing all of the items emitted by
   *         the source Observable.
   */
  def toList: Observable[List[T]] = to[List]

  /**
   * Returns an Observable that emits a single item, an `Iterable` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, an `Iterable` containing all of the items emitted by
   *         the source Observable.
   */
  def toIterable: Observable[Iterable[T]] = to[Iterable]

  /**
   * Returns an Observable that emits a single item, an `Iterator` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, an `Iterator` containing all of the items emitted by
   *         the source Observable.
   */
  def toIterator: Observable[Iterator[T]] = toIterable.map(_.iterator)

  /**
   * Returns an Observable that emits a single item, a `Stream` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `Stream` containing all of the items emitted by
   *         the source Observable.
   */
  def toStream: Observable[Stream[T]] = to[Stream]

  /**
   * Returns an Observable that emits a single item, an `IndexedSeq` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, an `IndexedSeq` containing all of the items emitted by
   *         the source Observable.
   */
  def toIndexedSeq: Observable[immutable.IndexedSeq[T]] = to[immutable.IndexedSeq]

  /**
   * Returns an Observable that emits a single item, a `Vector` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `Vector` containing all of the items emitted by
   *         the source Observable.
   */
  def toVector: Observable[Vector[T]] = to[Vector]

  /**
   * Returns an Observable that emits a single item, a `Buffer` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `Buffer` containing all of the items emitted by
   *         the source Observable.
   */
  def toBuffer[U >: T]: Observable[mutable.Buffer[U]] = { // use U >: T because Buffer is invariant
    val us: Observable[U] = this
    us.to[ArrayBuffer]
  }

  /**
   * Returns an Observable that emits a single item, a `Set` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, a `Set` containing all of the items emitted by
   *         the source Observable.
   */
  def toSet[U >: T]: Observable[immutable.Set[U]] = { // use U >: T because Set is invariant
    val us: Observable[U] = this
    us.to[immutable.Set]
  }

  /**
   * Returns an Observable that emits a single item, an `Array` composed of all the items emitted by
   * the source Observable.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item, an `Array` containing all of the items emitted by
   *         the source Observable.
   */
  def toArray[U >: T : ClassTag]: Observable[Array[U]] = // use U >: T because Array is invariant
    toBuffer[U].map(_.toArray)

  /**
   * Returns an [[Observable]] which only emits elements which do not satisfy a predicate.
   *
   * @param p the predicate used to test elements.
   * @return Returns an [[Observable]] which only emits elements which do not satisfy a predicate.
   */
  def filterNot(p: T => Boolean): Observable[T] = {
    filter(!p(_))
  }

  /**
   * Return an [[Observable]] which emits the number of elements in the source [[Observable]] which satisfy a predicate.
   *
   * @param p the predicate used to test elements.
   * @return an [[Observable]] which emits the number of elements in the source [[Observable]] which satisfy a predicate.
   */
  def count(p: T => Boolean): Observable[Int] = {
    filter(p).length
  }

  /**
   * Return an [[Observable]] emitting one single `Boolean`, which is `true` if the source [[Observable]] emits any element, and `false` otherwise.
   *
   * @return an [[Observable]] emitting one single Boolean`, which is `true` if the source  [[Observable]] emits any element, and `false otherwise.
   */
  def nonEmpty: Observable[Boolean] = {
    isEmpty.map(!_)
  }

  /**
   * Instructs an Observable that is emitting items faster than its observer can consume them to buffer these
   * items indefinitely until they can be emitted.
   *
   * <img width="640" height="300" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.buffer.png" alt="" />
   *
   * ===Scheduler:===
   * `onBackpressureBuffer` does not operate by default on a particular `Scheduler`.
   *
   * @return the source Observable modified to buffer items to the extent system resources allow
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Backpressure">RxJava wiki: Backpressure</a>
   */
  def onBackpressureBuffer: Observable[T] = {
    toScalaObservable[T](asJavaObservable.onBackpressureBuffer)
  }

  /**
   * $beta Instructs an [[Observable]] that is emitting items faster than its [[Observer]] can consume them to buffer up to
   * a given amount of items until they can be emitted. The resulting [[Observable]] will emit
   * `BufferOverflowException` as soon as the buffer's capacity is exceeded, drop all undelivered
   * items, and unsubscribe from the source.
   *
   * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.buffer.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param capacity capacity of the internal buffer.
   * @return an [[Observable]] that will buffer items up to the given capacity
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Beta
  def onBackpressureBuffer(capacity: Long): Observable[T] = {
    asJavaObservable.onBackpressureBuffer(capacity)
  }

  /**
   * $beta Instructs an [[Observable]] that is emitting items faster than its [[Observer]] can consume them to buffer up to
   * a given amount of items until they can be emitted. The resulting [[Observable]] will emit
   * `BufferOverflowException` as soon as the buffer's capacity is exceeded, drop all undelivered
   * items, unsubscribe from the source, and notify `onOverflow`.
   *
   * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.buffer.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param capacity capacity of the internal buffer.
   * @param onOverflow an action to run when the buffer's capacity is exceeded. This is a by-name parameter.
   * @return the source Observable modified to buffer items up to the given capacity
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Beta
  def onBackpressureBuffer(capacity: Long, onOverflow: => Unit): Observable[T] = {
    asJavaObservable.onBackpressureBuffer(capacity, new Action0 {
      override def call(): Unit = onOverflow
    })
  }

  /**
   * Use this operator when the upstream does not natively support backpressure and you wish to drop
   * `onNext` when unable to handle further events.
   *
   * <img width="640" height="245" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.drop.png" alt="" />
   *
   * If the downstream request count hits 0 then `onNext` will be dropped until `request(long n)`
   * is invoked again to increase the request count.
   *
   * ===Scheduler:===
   * onBackpressureDrop` does not operate by default on a particular `Scheduler`.
   *
   * @return the source Observable modified to drop `onNext` notifications on overflow
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Backpressure">RxJava wiki: Backpressure</a>
   */
  def onBackpressureDrop: Observable[T] = {
    toScalaObservable[T](asJavaObservable.onBackpressureDrop)
  }

  /**
   * $experimental Instructs an [[Observable]] that is emitting items faster than its observer can consume them to discard,
   * rather than emit, those items that its observer is not prepared to observe.
   *
   * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.drop.png" alt="">
   *
   * If the downstream request count hits `0` then the [[Observable]] will refrain from calling `onNext` until
   * the observer invokes `request(n)` again to increase the request count.
   *
   * $noDefaultScheduler
   *
   * @param onDrop the action to invoke for each item dropped. `onDrop` action should be fast and should never block.
   * @return an new [[Observable]] that will drop `onNext` notifications on overflow
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def onBackpressureDrop(onDrop: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.onBackpressureDrop(new Action1[T] {
      override def call(t: T) = onDrop(t)
    }))
  }

  /**
   * $experimental Instructs an Observable that is emitting items faster than its observer can consume them to
   * hold onto the latest value and emit that on request.
   * <p>
   * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.latest.png" alt="">
   * <p>
   * Its behavior is logically equivalent to {@code toBlocking().latest()} with the exception that
   * the downstream is not blocking while requesting more values.
   * <p>
   * Note that if the upstream Observable does support backpressure, this operator ignores that capability
   * and doesn't propagate any backpressure requests from downstream.
   * <p>
   * Note that due to the nature of how backpressure requests are propagated through subscribeOn/observeOn,
   * requesting more than 1 from downstream doesn't guarantee a continuous delivery of onNext events.
   *
   * @return the source Observable modified so that it emits the most recently-received item upon request
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def onBackpressureLatest: Observable[T] = {
    asJavaObservable.onBackpressureLatest
  }

  /**
   * Return a new [[Observable]] by applying a partial function to all elements of this [[Observable]]
   * on which the function is defined.
   *
   * @tparam R the element type of the returned [[Observable]].
   * @param pf the partial function which filters and maps the [[Observable]].
   * @return a new [[Observable]] by applying a partial function to all elements of this [[Observable]]
   *         on which the function is defined.
   */
  def collect[R](pf: PartialFunction[T, R]): Observable[R] = {
    filter(pf.isDefinedAt(_)).map(pf)
  }

  /**
   * $experimental Instructs an [[Observable]] will block the producer thread if the source emits items faster than its [[Observer]] can consume them
   *
   * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.block.png" alt="">
   *
   * The producer side can emit up to `maxQueueLength` onNext elements without blocking, but the
   * consumer side considers the amount its downstream requested through `Producer.request(n)`
   * and doesn't emit more than requested even if more is available. For example, using
   * `onBackpressureBlock(384).observeOn(Schedulers.io())` will not throw a MissingBackpressureException.
   *
   * Note that if the upstream Observable does support backpressure, this operator ignores that capability
   * and doesn't propagate any backpressure requests from downstream.
   *
   * $noDefaultScheduler
   *
   * @param maxQueueLength the maximum number of items the producer can emit without blocking
   * @return an [[Observable]] that will block the producer thread if the source emits items faster than its [[Observer]] can consume them
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  @deprecated("The operator doesn't work properly with [[Observable.subscribeOn]]` and is prone to deadlocks.It will be removed / unavailable in future", "0.25.1")
  def onBackpressureBlock(maxQueueLength: Int): Observable[T] = {
    asJavaObservable.onBackpressureBlock(maxQueueLength)
  }

  /**
   * $experimental Instructs an [[Observable]] will block the producer thread if the source emits items faster than its [[Observer]] can consume them
   *
   * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.block.png" alt="">
   *
   * The producer side can emit up to the system-wide ring buffer size onNext elements without blocking, but
   * the consumer side considers the amount its downstream requested through `Producer.request(n)`
   * and doesn't emit more than requested even if available.
   *
   * Note that if the upstream Observable does support backpressure, this operator ignores that capability
   * and doesn't propagate any backpressure requests from downstream.
   *
   * $noDefaultScheduler
   *
   * @return an [[Observable]] that will block the producer thread if the source emits items faster than its [[Observer]] can consume them
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  @deprecated("The operator doesn't work properly with [[Observable.subscribeOn]]` and is prone to deadlocks.It will be removed / unavailable in future", "0.25.1")
  def onBackpressureBlock: Observable[T] = {
    asJavaObservable.onBackpressureBlock()
  }

  /**
   * $beta An [[Observable]] wrapping the source one that will invokes the given action when it receives a request for more items.
   *
   * $noDefaultScheduler
   *
   * @param onRequest the action that gets called when an [[Observer]] requests items from this [[Observable]]
   * @return an [[Observable]] that will call `onRequest` when appropriate
   * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Beta
  def doOnRequest(onRequest: Long => Unit): Observable[T] = {
    asJavaObservable.doOnRequest(new Action1[java.lang.Long] {
      override def call(request: java.lang.Long): Unit = onRequest(request)
    })
  }

  /**
   * $experimental Merges the specified [[Observable]] into this [[Observable]] sequence by using the `resultSelector`
   * function only when the source [[Observable]] (this instance) emits an item.
   *
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param other the other [[Observable]]
   * @param resultSelector the function to call when this [[Observable]] emits an item and the other [[Observable]] has already
   *                       emitted an item, to generate the item to be emitted by the resulting [[Observable]]
   * @return an [[Observable]] that merges the specified [[Observable]] into this [[Observable]] by using the
   *         `resultSelector` function only when the source [[Observable]] sequence (this instance) emits an item
   * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @Experimental
  def withLatestFrom[U, R](other: Observable[U])(resultSelector: (T, U) => R): Observable[R] = {
    val func = new Func2[T, U, R] {
      override def call(t1: T, t2: U): R = resultSelector(t1, t2)
    }
    toScalaObservable[R](asJavaObservable.withLatestFrom(other.asJavaObservable, func))
  }

  /**
   * Instructs an [[Observable]] that is emitting items faster than its [[Observer]] can consume them to buffer up to
   * a given amount of items until they can be emitted. The resulting [[Observable]] will emit
   * `BufferOverflowException` as soon as the buffer's capacity is exceeded, drop all undelivered
   * items, and unsubscribe from the source.
   *
   * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.buffer.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param capacity capacity of the internal buffer.
   * @return an [[Observable]] that will buffer items up to the given capacity
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   */
  @deprecated("Use [[[Observable.onBackpressureBuffer(capacity:Long)*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureBufferWithCapacity(capacity: Long): Observable[T] = {
    asJavaObservable.onBackpressureBuffer(capacity)
  }

  /**
   * Instructs an [[Observable]] that is emitting items faster than its [[Observer]] can consume them to buffer up to
   * a given amount of items until they can be emitted. The resulting [[Observable]] will emit
   * `BufferOverflowException` as soon as the buffer's capacity is exceeded, drop all undelivered
   * items, unsubscribe from the source, and notify `onOverflow`.
   *
   * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.buffer.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param capacity capacity of the internal buffer.
   * @param onOverflow an action to run when the buffer's capacity is exceeded. This is a by-name parameter.
   * @return the source Observable modified to buffer items up to the given capacity
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  @deprecated("Use [[[Observable.onBackpressureBuffer(capacity:Long,onOverflow:=>Unit)*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureBufferWithCapacity(capacity: Long, onOverflow: => Unit): Observable[T] = {
    asJavaObservable.onBackpressureBuffer(capacity, new Action0 {
      override def call(): Unit = onOverflow
    })
  }

  /**
   * Returns an [[Observable]] that emits items based on applying a function that you supply to each item emitted
   * by the source [[Observable]] , where that function returns an [[Observable]] , and then merging those resulting
   * [[Observable]]s and emitting the results of this merger, while limiting the maximum number of concurrent
   * subscriptions to these [[Observable]]s.
   *
   * $$noDefaultScheduler
   *
   * @param f a function that, when applied to an item emitted by the source [[Observable]], returns an [[Observable]]
   * @param maxConcurrent the maximum number of [[Observable]]s that may be subscribed to concurrently
   * @return an [[Observable]] that emits the result of applying the transformation function to each item emitted
   *         by the source [[Observable]] and merging the results of the [[Observable]]s obtained from this transformation
   * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
   */
  @deprecated("Use [[[Observable.flatMap[R](maxConcurrent:Int,f:T=>rx\\.lang\\.scala\\.Observable[R])*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def flatMapWithMaxConcurrent[R](f: T => Observable[R], maxConcurrent: Int): Observable[R] = {
    toScalaObservable[R](asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]] {
      def call(t1: T): rx.Observable[_ <: R] = {
        f(t1).asJavaObservable
      }
    }, maxConcurrent))
  }

  /**
   * Returns an [[Observable]] that applies a function to each item emitted or notification raised by the source
   * [[Observable]]  and then flattens the [[Observable]] s returned from these functions and emits the resulting items,
   * while limiting the maximum number of concurrent subscriptions to these [[Observable]]s.
   *
   * $noDefaultScheduler
   *
   * @param onNext a function that returns an [[Observable]] to merge for each item emitted by the source [[Observable]]
   * @param onError a function that returns an [[Observable]] to merge for an onError notification from the source [[Observable]]
   * @param onCompleted a function that returns an [[Observable]] to merge for an onCompleted notification from the source [[Observable]]
   * @param maxConcurrent the maximum number of [[Observable]]s that may be subscribed to concurrently
   * @return an [[Observable]] that emits the results of merging the [[Observable]]s returned from applying the
   *         specified functions to the emissions and notifications of the source [[Observable]]
   * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
   */
  @deprecated("Use [[[Observable.flatMap[R](maxConcurrent:Int,onNext:T=>rx\\.lang\\.scala\\.Observable[R],onError:Throwable=>rx\\.lang\\.scala\\.Observable[R],onCompleted:()=>rx\\.lang\\.scala\\.Observable[R])*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def flatMapWithMaxConcurrent[R](onNext: T => Observable[R], onError: Throwable => Observable[R], onCompleted: () => Observable[R], maxConcurrent: Int): Observable[R] = {
    val jOnNext = new Func1[T, rx.Observable[_ <: R]] {
      override def call(t: T): rx.Observable[_ <: R] = onNext(t).asJavaObservable
    }
    val jOnError = new Func1[Throwable, rx.Observable[_ <: R]] {
      override def call(e: Throwable): rx.Observable[_ <: R] = onError(e).asJavaObservable
    }
    val jOnCompleted = new Func0[rx.Observable[_ <: R]] {
      override def call(): rx.Observable[_ <: R] = onCompleted().asJavaObservable
    }
    toScalaObservable[R](asJavaObservable.flatMap[R](jOnNext, jOnError, jOnCompleted, maxConcurrent))
  }

  /**
   * Instructs an [[Observable]] that is emitting items faster than its observer can consume them to discard,
   * rather than emit, those items that its observer is not prepared to observe.
   *
   * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/bp.obp.drop.png" alt="">
   *
   * If the downstream request count hits `0` then the [[Observable]] will refrain from calling `onNext` until
   * the observer invokes `request(n)` again to increase the request count.
   *
   * $noDefaultScheduler
   *
   * @param onDrop the action to invoke for each item dropped. `onDrop` action should be fast and should never block.
   * @return an new [[Observable]] that will drop `onNext` notifications on overflow
   * @see <a href="http://reactivex.io/documentation/operators/backpressure.html">ReactiveX operators documentation: backpressure operators</a>
   */
  @deprecated("Use [[[Observable.onBackpressureDrop(onDrop:T=>Unit)*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureDropDo(onDrop: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.onBackpressureDrop(new Action1[T] {
      override def call(t: T) = onDrop(t)
    }))
  }
}

/**
 * Provides various ways to construct new Observables.
 *
 * @define noDefaultScheduler
 * ===Scheduler:===
 * This method does not operate by default on a particular [[Scheduler]].
 *
 */
object Observable {
  import scala.collection.JavaConverters._
  import scala.collection.immutable.Range
  import scala.concurrent.duration.Duration
  import scala.concurrent.{Future, ExecutionContext}
  import scala.util.{Success, Failure}
  import ImplicitFunctionConversions._
  import JavaConversions._
  import rx.lang.scala.subjects.AsyncSubject

  private[scala]
  def jObsOfListToScObsOfSeq[T](jObs: rx.Observable[_ <: java.util.List[T]]): Observable[Seq[T]] = {
    val oScala1: Observable[java.util.List[T]] = new Observable[java.util.List[T]]{ val asJavaObservable = jObs }
    oScala1.map((lJava: java.util.List[T]) => lJava.asScala)
  }

  private[scala]
  def jObsOfJObsToScObsOfScObs[T](jObs: rx.Observable[_ <: rx.Observable[_ <: T]]): Observable[Observable[T]] = {
    val oScala1: Observable[rx.Observable[_ <: T]] = new Observable[rx.Observable[_ <: T]]{ val asJavaObservable = jObs }
    oScala1.map((oJava: rx.Observable[_ <: T]) => oJava)
  }

  /**
   * Creates an Observable that will execute the given function when an [[rx.lang.scala.Observer]] subscribes to it.
   *
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/create.png" alt="" />
   *
   * Write the function you pass to `create` so that it behaves as an Observable: It
   * should invoke the Observer's [[rx.lang.scala.Observer.onNext onNext]], [[rx.lang.scala.Observer.onError onError]], and [[rx.lang.scala.Observer.onCompleted onCompleted]] methods
   * appropriately.
   *
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
   * for detailed information.
   *
   *
   * @tparam T
   *            the type of the items that this Observable emits.
   * @param f
   *            a function that accepts an `Observer[T]`, invokes its `onNext`, `onError`, and `onCompleted` methods
   *            as appropriate, and returns a [[rx.lang.scala.Subscription]] to allow the Observer to
   *            canceling the subscription.
   * @return
   *         an Observable that, when an [[rx.lang.scala.Observer]] subscribes to it, will execute the given function.
   */
  def create[T](@deprecatedName('func) f: Observer[T] => Subscription): Observable[T] = {
    Observable(
      (subscriber: Subscriber[T]) => {
        val s = f(subscriber)
        if (s != null && s != subscriber) {
          subscriber.add(s)
        }
      }
    )
  }

  /*
  Note: It's dangerous to have two overloads where one takes an `Observer[T] => Subscription`
  function and the other takes a `Subscriber[T] => Unit` function, because expressions like
  `o => Subscription{}` have both of these types.
  So we call the old create method "create", and the new create method "apply".
  Try it out yourself here:
  def foo[T]: Unit = { 
    val fMeant: Observer[T] => Subscription = o => Subscription{}
    val fWrong: Subscriber[T] => Unit = o => Subscription{}
  }
  */

  /**
   * Returns an Observable that will execute the specified function when someone subscribes to it.
   *
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/create.png" alt="" />
   *
   * Write the function you pass so that it behaves as an Observable: It should invoke the
   * Subscriber's `onNext`, `onError`, and `onCompleted` methods appropriately.
   *
   * You can `add` custom [[Subscription]]s to [[Subscriber]]. These [[Subscription]]s will be called
   * <ul>
   *   <li>when someone calls `unsubscribe`.</li>
   *   <li>after `onCompleted` or `onError`.</li>
   * </ul>
   *
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed
   * information.
   *
   * See `<a href="https://github.com/ReactiveX/RxScala/blob/0.x/examples/src/test/scala/examples/RxScalaDemo.scala">RxScalaDemo</a>.createExampleGood`
   * and `<a href="https://github.com/ReactiveX/RxScala/blob/0.x/examples/src/test/scala/examples/RxScalaDemo.scala">RxScalaDemo</a>.createExampleGood2`.
   *
   * @tparam T
   *            the type of the items that this Observable emits
   * @param f
   *            a function that accepts a `Subscriber[T]`, and invokes its `onNext`,
   *            `onError`, and `onCompleted` methods as appropriate
   * @return an Observable that, when someone subscribes to it, will execute the specified
   *         function
   */
  def apply[T](f: Subscriber[T] => Unit): Observable[T] = {
    toScalaObservable(rx.Observable.create(f))
  }

  /**
   * Returns an [[Observable]] that invokes an [[Observer.onError]] method when the [[Observer]] subscribes to it.
   *
   * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.png" alt="">
   *
   * $noDefaultScheduler
   *
   * @param exception the particular `Throwable` to pass to [[Observer.onError]]
   * @return an [[Observable]] that invokes the [[Observer.onError]] method when the [[Observer]] subscribes to it
   * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
   */
  def error(exception: Throwable): Observable[Nothing] = {
    toScalaObservable[Nothing](rx.Observable.error(exception))
  }

  /**
   * Returns an Observable that emits no data to the [[rx.lang.scala.Observer]] and
   * immediately invokes its [[rx.lang.scala.Observer#onCompleted onCompleted]] method
   * with the specified scheduler.
   * <p>
   * <img width="640" height="190" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/empty.s.png" alt="" />
   *
   * @return an Observable that returns no data to the [[rx.lang.scala.Observer]] and
   *         immediately invokes the [[rx.lang.scala.Observer]]r's
   *        [[rx.lang.scala.Observer#onCompleted onCompleted]] method with the
   *         specified scheduler
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#empty-error-and-never">RxJava Wiki: empty()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229066.aspx">MSDN: Observable.Empty Method (IScheduler)</a>
   */
  def empty: Observable[Nothing] = {
    toScalaObservable(rx.Observable.empty[Nothing]())
  }

  /**
   * Converts a sequence of values into an Observable.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/from.png" alt="" />
   *
   * Implementation note: the entire array will be immediately emitted each time an [[rx.lang.scala.Observer]] subscribes.
   * Since this occurs before the [[rx.lang.scala.Subscription]] is returned,
   * it in not possible to unsubscribe from the sequence before it completes.
   *
   * @param items
   *            the source Array
   * @tparam T
   *            the type of items in the Array, and the type of items to be emitted by the
   *            resulting Observable
   * @return an Observable that emits each item in the source Array
   */
  def just[T](items: T*): Observable[T] = {
    toScalaObservable[T](rx.Observable.from(items.toIterable.asJava))
  }

 /** Returns an Observable emitting the value produced by the Future as its single item.
   * If the future fails, the Observable will fail as well.
   *
   * @param f Future whose value ends up in the resulting Observable
   * @return an Observable completed after producing the value of the future, or with an exception
   */
  def from[T](f: Future[T])(implicit execContext: ExecutionContext): Observable[T] = {
    val s = AsyncSubject[T]()
    f.onComplete {
      case Failure(e) =>
        s.onError(e)
      case Success(c) =>
        s.onNext(c)
        s.onCompleted()
    }
    s
  }

  /**
   * Converts an `Iterable` into an Observable.
   *
   * <img width="640" height="315" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/from.png" alt="" />
   *
   * Note: the entire iterable sequence is immediately emitted each time an
   * Observer subscribes. Since this occurs before the
   * `Subscription` is returned, it is not possible to unsubscribe from
   * the sequence before it completes.
   *
   * @param iterable the source `Iterable` sequence
   * @tparam T the type of items in the `Iterable` sequence and the
   *            type of items to be emitted by the resulting Observable
   * @return an Observable that emits each item in the source `Iterable`
   *         sequence
   */
  def from[T](iterable: Iterable[T]): Observable[T] = {
    toScalaObservable(rx.Observable.from(iterable.asJava))
  }

  /**
   * Returns an Observable that calls an Observable factory to create its Observable for each
   * new Observer that subscribes. That is, for each subscriber, the actual Observable is determined
   * by the factory function.
   *
   * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/defer.png" alt="" />
   *
   * The defer operator allows you to defer or delay emitting items from an Observable until such
   * time as an Observer subscribes to the Observable. This allows an [[rx.lang.scala.Observer]] to easily
   * obtain updates or a refreshed version of the sequence.
   *
   * @param observable
   *            the Observable factory function to invoke for each [[rx.lang.scala.Observer]] that
   *            subscribes to the resulting Observable
   * @tparam T
   *            the type of the items emitted by the Observable
   * @return an Observable whose [[rx.lang.scala.Observer]]s trigger an invocation of the given Observable
   *         factory function
   */
  def defer[T](observable: => Observable[T]): Observable[T] = {
    toScalaObservable[T](rx.Observable.defer[T](() => observable.asJavaObservable.asInstanceOf[rx.Observable[T]]))
  }

  /**
   * Returns an Observable that never sends any items or notifications to an [[rx.lang.scala.Observer]].
   *
   * <img width="640" height="185" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/never.png" alt="" />
   *
   * This Observable is useful primarily for testing purposes.
   *
   * @return an Observable that never sends any items or notifications to an [[rx.lang.scala.Observer]]
   */
  def never: Observable[Nothing] = {
    toScalaObservable[Nothing](rx.Observable.never())
  }

  /**
   * Given 3 observables, returns an observable that emits Tuples of 3 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   *
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C](obA: Observable[A], obB: Observable[B], obC: Observable[C]): Observable[(A, B, C)] = {
    toScalaObservable[(A, B, C)](rx.Observable.zip[A, B, C, (A, B, C)](obA.asJavaObservable, obB.asJavaObservable, obC.asJavaObservable, (a: A, b: B, c: C) => (a, b, c)))
  }

  /**
   * Given 4 observables, returns an observable that emits Tuples of 4 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   *
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C, D](obA: Observable[A], obB: Observable[B], obC: Observable[C], obD: Observable[D]): Observable[(A, B, C, D)] = {
    toScalaObservable[(A, B, C, D)](rx.Observable.zip[A, B, C, D, (A, B, C, D)](obA.asJavaObservable, obB.asJavaObservable, obC.asJavaObservable, obD.asJavaObservable, (a: A, b: B, c: C, d: D) => (a, b, c, d)))
  }

  /**
   * Given an Observable emitting `N` source observables, returns an observable that
   * emits Seqs of `N` elements each.
   * The first emitted Seq will contain the first element of each source observable,
   * the second Seq the second element of each source observable, and so on.
   *
   * Note that the returned Observable will only start emitting items once the given
   * `Observable[Observable[T]]` has completed, because otherwise it cannot know `N`.
   *
   * @param observables
   *            An Observable emitting N source Observables
   * @return an Observable that emits the zipped Seqs
   */
  def zip[T](observables: Observable[Observable[T]]): Observable[Seq[T]] = {
    val f: FuncN[Seq[T]] = (args: Seq[java.lang.Object]) => {
      val asSeq: Seq[Object] = args.toSeq
      asSeq.asInstanceOf[Seq[T]]
    }
    val list = observables.map(_.asJavaObservable).asJavaObservable
    val o = rx.Observable.zip(list, f)
    toScalaObservable[Seq[T]](o)
  }

  /**
   * Emits `0`, `1`, `2`, `...` with a delay of `duration` between consecutive numbers.
   *
   * <img width="640" height="195" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.png" alt="" />
   *
   * @param duration
   *            duration between two consecutive numbers
   * @return An Observable that emits a number each time interval.
   */
  def interval(duration: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(duration.length, duration.unit)).map(_.longValue())
  }

  /**
   * Emits `0`, `1`, `2`, `...` with a delay of `duration` between consecutive numbers.
   *
   * <img width="640" height="195" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.png" alt="" />
   *
   * @param period
   *            duration between two consecutive numbers
   * @param scheduler
   *            the scheduler to use
   * @return An Observable that emits a number each time interval.
   */
  def interval(period: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(period.length, period.unit, scheduler)).map(_.longValue())
  }

  /**
   * Returns an [[Observable]] that emits a `0L` after the `initialDelay` and ever increasing numbers
   * after each `period` of time thereafter.
   *
   * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.p.png" alt="" />
   *
   * ===Backpressure Support:===
   * This operator does not support backpressure as it uses time. If the downstream needs a slower rate
   * it should slow the timer or use something like [[Observable.onBackpressureDrop:* onBackpressureDrop]].
   *
   * ===Scheduler:===
   * `interval` operates by default on the `computation` [[Scheduler]].
   *
   * @param initialDelay the initial delay time to wait before emitting the first value of 0L
   * @param period the period of time between emissions of the subsequent numbers
   * @return an [[Observable]] that emits a `0L` after the `initialDelay` and ever increasing numbers after
   *         each `period` of time thereafter
   * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
   */
  def interval(initialDelay: Duration, period: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS)).map(_.longValue())
  }

  /**
   * Returns an [[Observable]] that emits a `0L` after the `initialDelay` and ever increasing numbers
   * after each `period` of time thereafter, on a specified [[Scheduler]].
   *
   * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.ps.png" alt="" />
   *
   * ===Backpressure Support:===
   * This operator does not support backpressure as it uses time. If the downstream needs a slower rate
   * it should slow the timer or use something like [[Observable.onBackpressureDrop:* onBackpressureDrop]].
   *
   * ===Scheduler:===
   * you specify which [[Scheduler]] this operator will use.
   *
   * @param initialDelay the initial delay time to wait before emitting the first value of `0L`
   * @param period the period of time between emissions of the subsequent numbers
   * @param scheduler the [[Scheduler]] on which the waiting happens and items are emitted
   * @return an [[Observable]] that emits a `0L` after the `initialDelay` and ever increasing numbers after
   *         each `period` of time thereafter, while running on the given [[Scheduler]]
   * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
   */
  def interval(initialDelay: Duration, period: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS, scheduler)).map(_.longValue())
  }

  /**
   * Return an Observable that emits a 0L after the `initialDelay` and ever increasing
   * numbers after each `period` of time thereafter, on a specified Scheduler.
   * <p>
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.ps.png" alt="" />
   *
   * @param initialDelay
   * the initial delay time to wait before emitting the first value of 0L
   * @param period
   * the period of time between emissions of the subsequent numbers
   * @return an Observable that emits a 0L after the `initialDelay` and ever increasing
   *         numbers after each `period` of time thereafter, while running on the given `scheduler`
   */
  @deprecated("Use [[Observable$.interval(initialDelay:scala\\.concurrent\\.duration\\.Duration,period:scala\\.concurrent\\.duration\\.Duration)* interval]] instead.", "0.25.1")
  def timer(initialDelay: Duration, period: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS)).map(_.longValue())
  }

  /**
   * Return an Observable that emits a 0L after the `initialDelay` and ever increasing
   * numbers after each `period` of time thereafter, on a specified Scheduler.
   * <p>
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.ps.png" alt="" />
   *
   * @param initialDelay
   * the initial delay time to wait before emitting the first value of 0L
   * @param period
   * the period of time between emissions of the subsequent numbers
   * @param scheduler
   * the scheduler on which the waiting happens and items are emitted
   * @return an Observable that emits a 0L after the `initialDelay` and ever increasing
   * numbers after each `period` of time thereafter, while running on the given `scheduler`
   */
  @deprecated("Use [[Observable$.interval(initialDelay:scala\\.concurrent\\.duration\\.Duration,period:scala\\.concurrent\\.duration\\.Duration,scheduler:rx\\.lang\\.scala\\.Scheduler)* interval]] instead.", "0.25.1")
  def timer(initialDelay: Duration, period: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS, scheduler)).map(_.longValue())
  }

  /**
   * Returns an Observable that emits `0L` after a specified delay, and then completes.
   *
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.png" alt="" />
   *
   * @param delay the initial delay before emitting a single `0L`
   * @return Observable that emits `0L` after a specified delay, and then completes
   */
  def timer(delay: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(delay.length, delay.unit)).map(_.longValue())
  }

  /**
   * Returns an Observable that emits `0L` after a specified delay, on a specified Scheduler, and then
   * completes.
   *
   * <img width="640" height="200" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.s.png" alt="" />
   *
   * @param delay the initial delay before emitting a single `0L`
   * @param scheduler the Scheduler to use for scheduling the item
   * @return Observable that emits `0L` after a specified delay, on a specified Scheduler, and then completes
   */
  def timer(delay: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(delay.length, delay.unit, scheduler)).map(_.longValue())
  }

  /**
   * Constructs an Observable that creates a dependent resource object.
   *
   * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/using.png" alt="" />
   *
   * ===Scheduler:===
   * `using` does not operate by default on a particular `Scheduler`.
   *
   * @param resourceFactory the factory function to create a resource object that depends on the Observable.
   *                        Note: this is a by-name parameter.
   * @param observableFactory the factory function to create an Observable
   * @param dispose the function that will dispose of the resource
   * @param disposeEagerly if `true` then disposal will happen either on unsubscription or just before emission of
   *                       a terminal event (`onComplete` or `onError`).
   * @return the Observable whose lifetime controls the lifetime of the dependent resource object
   * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#using">RxJava wiki: using</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229585.aspx">MSDN: Observable.Using</a>
   * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
   */
  def using[T, Resource](resourceFactory: => Resource)(observableFactory: Resource => Observable[T], dispose: Resource => Unit, disposeEagerly: Boolean = false): Observable[T] = {
    val jResourceFactory = new rx.functions.Func0[Resource] {
      override def call: Resource = resourceFactory
    }
    val jObservableFactory = new rx.functions.Func1[Resource, rx.Observable[_ <: T]] {
      override def call(r: Resource) = observableFactory(r).asJavaObservable
    }
    toScalaObservable[T](rx.Observable.using[T, Resource](jResourceFactory, jObservableFactory, dispose, disposeEagerly))
  }

  /**
   * Mirror the one Observable in an Iterable of several Observables that first emits an item.
   *
   * <img width="640" height="385" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/amb.png" alt="" />
   *
   * @param sources an Iterable of Observable sources competing to react first
   * @return an Observable that emits the same sequence of items as whichever of the source Observables
   *         first emitted an item
   */
  def amb[T](sources: Observable[T]*): Observable[T] = {
    toScalaObservable[T](rx.Observable.amb[T](sources.map(_.asJavaObservable).asJava))
  }

  /**
   * Combines a list of source Observables by emitting an item that aggregates the latest values of each of
   * the source Observables each time an item is received from any of the source Observables, where this
   * aggregation is defined by a specified function.
   *
   * @tparam T the common base type of source values
   * @tparam R the result type
   * @param sources the list of source Observables
   * @param combineFunction the aggregation function used to combine the items emitted by the source Observables
   * @return an Observable that emits items that are the result of combining the items emitted by the source
   *         Observables by means of the given aggregation function
   */
  def combineLatest[T, R](sources: Seq[Observable[T]])(combineFunction: Seq[T] => R): Observable[R] = {
    val jSources = new java.util.ArrayList[rx.Observable[_ <: T]](sources.map(_.asJavaObservable).asJava)
    val jCombineFunction = new rx.functions.FuncN[R] {
      override def call(args: java.lang.Object*): R = combineFunction(args.map(_.asInstanceOf[T]))
    }
    toScalaObservable[R](rx.Observable.combineLatest[T, R](jSources, jCombineFunction))
  }
}

