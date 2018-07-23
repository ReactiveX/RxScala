package rx.lang.scala

import rx.annotations.Experimental
import rx.lang.scala.observables.OnSubscribe
import scala.collection.Iterable
import scala.util.Try

trait Single[+T] {

  import ImplicitFunctionConversions._
  import JavaConversions._
  import rx.functions._
  import rx.lang.scala.observables.BlockingObservable

  import scala.concurrent.duration.Duration

  private [scala] val asJavaSingle: rx.Single[_ <: T]

  /**
    * Subscribes to an [[Single]] but ignore its emissions and notifications.
    *
    * $noDefaultScheduler
    *
    * @return $subscribeAllReturn
    * @throws rx.exceptions.OnErrorNotImplementedException if the [[Single]] tries to call `onError`
    * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
    */
  def subscribe(): Subscription = {
    asJavaSingle.subscribe()
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
    asJavaSingle.subscribe(observer.asJavaObserver)
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
    val thisJava = asJavaSingle.asInstanceOf[rx.Single[T]]
    thisJava.subscribe(subscriber.asJavaSubscriber)
  }

  /**
    * Subscribe to Single and invoke `OnSubscribe` function without any
    * contract protection, error handling, unsubscribe, or execution hooks.
    *
    * This should only be used for implementing an `Operator` that requires nested subscriptions.
    *
    * Normal use should use `Single.subscribe` which ensures the Rx contract and other functionality.
    *
    * @param subscriber
    * @return [[Subscription]] which is the Subscriber passed in
    * @since 0.17
    */
  def unsafeSubscribe(subscriber: Subscriber[T]): Subscription = {
    asJavaSingle.unsafeSubscribe(subscriber.asJavaSubscriber)
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
    asJavaSingle.subscribe(scalaFunction1ProducingUnitToAction1(onNext))
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
    asJavaSingle.subscribe(
      scalaFunction1ProducingUnitToAction1(onNext),
      scalaFunction1ProducingUnitToAction1(onError)
    )
  }


  /**
    * Returns an Observable that first emits the items emitted by `this`, and then `elem`.
    *
    * @param elem the item to be appended
    * @return  an Observable that first emits the items emitted by `this`, and then `elem`.
    */
  def :+[U >: T](elem: U): Observable[U] = {

     asJavaSingle ++ Single.just(elem)
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
  def ++[U >: T](that: Single[U]): Observable[U] = {

    val thisJava = this.asJavaSingle.toObservable.asInstanceOf[rx.Observable[U]]
    val thatJava = that.asJavaSingle.toObservable.asInstanceOf[rx.Observable[U]]

    thisJava ++ thatJava
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
    val thisJava = this.asJavaSingle.toObservable.asInstanceOf[rx.Observable[U]]
    elem +: thisJava

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

    toScalaObservable(asJavaSingle.toObservable).zipWith(that)(selector)

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
    toScalaObservable(asJavaSingle.toObservable).zipWith(that)(selector)
  }

  /**
    * Registers an function to be called when this [[Observable]] invokes either [[Observer.onCompleted onCompleted]] or [[Observer.onError onError]].
    *
    * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/finallyDo.png" alt="">
    *
    * $noDefaultScheduler
    *
    * @param action an function to be invoked when the source [[Observable]] finishes
    * @return an [[Observable]] that emits the same items as the source [[Observable]], then invokes the `action`
    * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
    */
  def doAfterTerminate(action: => Unit): Single[T] = {
    toScalaSingle[T](asJavaSingle.doAfterTerminate(() => action))
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
  def flatMap[R](f: T => Single[R]): Single[R] = {
    toScalaSingle[R](asJavaSingle.flatMap[R](new Func1[T, rx.Single[_ <: R]]{
      def call(t1: T): rx.Single[_ <: R] = { f(t1).asJavaSingle}
    }))
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
  def flatMapObservable[R](f: T => Single[R]): Single[R] = {
    toScalaSingle[R](asJavaSingle.flatMap[R](new Func1[T, rx.Single[_ <: R]]{
      def call(t1: T): rx.Single[_ <: R] = { f(t1).asJavaSingle}
    }))
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
  def flatMapCompletable[R](f: T => Single[R]): Single[R] = {
    toScalaSingle[R](asJavaSingle.flatMap[R](new Func1[T, rx.Single[_ <: R]]{
      def call(t1: T): rx.Single[_ <: R] = { f(t1).asJavaSingle}
    }))
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
  def map[R](func: T => R): Single[R] = {
    toScalaSingle[R](asJavaSingle.map[R](new Func1[T,R] {
      def call(t1: T): R = func(t1)
    }))
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
  def subscribeOn(scheduler: Scheduler): Single[T] = {
    toScalaSingle[T](asJavaSingle.subscribeOn(scheduler))
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
  def observeOn(scheduler: Scheduler): Single[T] = {
    toScalaSingle[T](asJavaSingle.observeOn(scheduler))
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
    * @return the original Single, with appropriately modified behavior
    */
  def onErrorResumeNext[U >: T](resumeFunction: Throwable => Single[U]): Single[U] = {
    val f: Func1[Throwable, rx.Single[_ <: U]] = (t: Throwable) => resumeFunction(t).asJavaSingle
    val f2 = f.asInstanceOf[Func1[Throwable, rx.Single[Nothing]]]
    toScalaSingle[U](asJavaSingle.onErrorResumeNext(f2))
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
  def onErrorReturn[U >: T](resumeFunction: Throwable => U): Single[U] = {
    val f1: Func1[Throwable, _ <: U] = resumeFunction
    val f2 = f1.asInstanceOf[Func1[Throwable, Nothing]]
    toScalaSingle[U](asJavaSingle.onErrorReturn(f2))
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
  def cache: Single[T] = {
    toScalaSingle[T](asJavaSingle.cache())
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
  def takeUntil(that: Observable[Any]): Single[T] = {
    toScalaSingle[T](asJavaSingle.takeUntil(that.asJavaObservable))
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
    toScalaObservable[U](asJavaSingle.toObservable) merge that
  }

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
  def merge[U >: T](that: Single[U]): Observable[U] = {
    toScalaObservable[U](asJavaSingle.toObservable) merge  toScalaObservable[U](that.asJavaSingle.toObservable)
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
  def timeout(timeout: Duration): Single[T] = {
    toScalaSingle[T](asJavaSingle.timeout(timeout.length, timeout.unit))
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
  def timeout[U >: T](timeout: Duration, other: Single[U]): Single[U] = {
    val otherJava: rx.Single[_ <: U] = other.asJavaSingle
    val thisJava = this.asJavaSingle.asInstanceOf[rx.Single[U]]
    toScalaSingle[U](thisJava.timeout(timeout.length, timeout.unit, otherJava))
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
  def timeout(timeout: Duration, scheduler: Scheduler): Single[T] = {
    toScalaSingle[T](asJavaSingle.timeout(timeout.length, timeout.unit, scheduler.asJavaScheduler))
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
  def timeout[U >: T](timeout: Duration, other: Single[U], scheduler: Scheduler): Single[U] = {
    val otherJava: rx.Single[_ <: U] = other.asJavaSingle
    val thisJava = this.asJavaSingle.asInstanceOf[rx.Single[U]]
    toScalaSingle[U](thisJava.timeout(timeout.length, timeout.unit, otherJava, scheduler.asJavaScheduler))
  }


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
  def retry(retryCount: Long): Single[T] = {
    toScalaSingle[T](asJavaSingle.retry(retryCount))
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
  def retry: Single[T] = {
    toScalaSingle[T](asJavaSingle.retry())
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
  def retry(predicate: (Int, Throwable) => Boolean): Single[T] = {
    val f = new Func2[java.lang.Integer, Throwable, java.lang.Boolean] {
      def call(times: java.lang.Integer, e: Throwable): java.lang.Boolean = predicate(times, e)
    }
    toScalaSingle[T](asJavaSingle.retry(f))
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
    new BlockingObservable[T](asJavaSingle.toObservable)
  }

  /**
    * Invokes an action if the source Observable calls `onError`.
    *
    * @param onError the action to invoke if the source Observable calls
    *                `onError`
    * @return the source Observable with the side-effecting behavior applied
    */
  def doOnError(onError: Throwable => Unit): Single[T] = {
    toScalaSingle[T](asJavaSingle.doOnError(onError))
  }

  /**
    * Returns an Observable that applies the given function to each item emitted by an
    * Observable.
    *
    * @param onNext this function will be called whenever the Observable emits an item
    *
    * @return an Observable with the side-effecting behavior applied.
    */
  def doOnSuccess(onNext: T => Unit): Single[T] = {

    toScalaSingle[T](asJavaSingle.doOnSuccess(onNext))
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
  def doOnSubscribe(onSubscribe: => Unit): Single[T] = {
    toScalaSingle[T](asJavaSingle.doOnSubscribe(() => onSubscribe))
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
  def doOnUnsubscribe(onUnsubscribe: => Unit): Single[T] = {
    toScalaSingle[T](asJavaSingle.doOnUnsubscribe(() => onUnsubscribe))
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
  def delay(delay: Duration): Single[T] = {
    toScalaSingle[T](asJavaSingle.delay(delay.length, delay.unit))
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
  def delay(delay: Duration, scheduler: Scheduler): Single[T] = {
    toScalaSingle[T](asJavaSingle.delay(delay.length, delay.unit, scheduler))
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
  def delaySubscription(subscriptionDelay: Observable[Any]): Single[T] = {

    toScalaSingle[T](asJavaSingle.delaySubscription(subscriptionDelay))
  }


}

object Single {
  import ImplicitFunctionConversions._
  import JavaConversions._

  import scala.concurrent.{ExecutionContext, Future}


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
    * $experimental Returns an [[Observable]] that respects the back-pressure semantics. When the returned [[Observable]] is
    * subscribed to it will initiate the given [[observables.SyncOnSubscribe SyncOnSubscribe]]'s life cycle for generating events.
    *
    * Note: the [[observables.SyncOnSubscribe SyncOnSubscribe]] provides a generic way to fulfill data by iterating
    * over a (potentially stateful) function (e.g. reading data off of a channel, a parser). If your
    * data comes directly from an asynchronous/potentially concurrent source then consider using [[observables.AsyncOnSubscribe AsyncOnSubscribe]].
    *
    * $supportBackpressure
    *
    * $noDefaultScheduler
    *
    * @tparam T the type of the items that this [[Observable]] emits
    * @param OnSubscribe an implementation of [[observables.SyncOnSubscribe SyncOnSubscribe]] There are many creation methods on the object for convenience.
    * @return an [[Observable]] that, when a [[Subscriber]] subscribes to it, will use the specified [[observables.SyncOnSubscribe SyncOnSubscribe]] to generate events
    * @see [[observables.SyncOnSubscribe.stateful]]
    * @see [[observables.SyncOnSubscribe.singleState]]
    * @see [[observables.SyncOnSubscribe.stateless]]
    */
  @Experimental
  def create[T](onSubscribe: OnSubscribe[T]): Single[T] = toScalaSingle[T](rx.Single.create(onSubscribe))

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
  def apply[T](f: Subscriber[T] => Unit): Single[T] = {
    toScalaSingle(rx.Observable.create(f).toSingle)
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
  def error(exception: Throwable): Single[Nothing] = {
    toScalaSingle[Nothing](rx.Single.error(exception))
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
    * @param value
    *            the source value
    * @tparam T
    *            the type of items in the Array, and the type of items to be emitted by the
    *            resulting Observable
    * @return an Observable that emits each item in the source Array
    */
  def just[T](value: T): Single[T] = {
    toScalaSingle[T](rx.Single.just(value))
  }

  /** Returns an Observable emitting the value produced by the Future as its single item.
    * If the future fails, the Observable will fail as well.
    *
    * @param f Future whose value ends up in the resulting Observable
    * @return an Observable completed after producing the value of the future, or with an exception
    */
  def from[T](f: Future[T])(implicit execContext: ExecutionContext): Single[T] = {
    Observable.from(f).toSingle
  }


  /**
    * Converts a `Try` into an `Single`.
    *
    * Implementation note: the value will be immediately emitted each time an [[rx.lang.scala.Observer]] subscribes.
    * Since this occurs before the [[rx.lang.scala.Subscription]] is returned,
    * it in not possible to unsubscribe from the sequence before it completes.
    *
    * @param t the source Try
    * @tparam T the type of value in the Try, and the type of items to be emitted by the resulting Observable
    * @return an Single that either emits the value or the error in the Try.
    */
  def from[T](t: Try[T]): Single[T] = {
   Observable.from(t).toSingle
  }

  /**
    * Returns an Single that calls an Single factory to create its Single for each
    * new Observer that subscribes. That is, for each subscriber, the actual Single is determined
    * by the factory function.
    *
    * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/defer.png" alt="" />
    *
    * The defer operator allows you to defer or delay emitting items from an Single until such
    * time as an Observer subscribes to the Single. This allows an [[rx.lang.scala.Single]] to easily
    * obtain updates or a refreshed version of the sequence.
    *
    * @param single
    *            the Observable factory function to invoke for each [[rx.lang.scala.Single]] that
    *            subscribes to the resulting Observable
    * @tparam T
    *            the type of the items emitted by the Observable
    * @return an Observable whose [[rx.lang.scala.Single]]s trigger an invocation of the given Single
    *         factory function
    */
  def defer[T](single: => Single[T]): Single[T] = {
    toScalaSingle[T](rx.Single.defer[T](() => single.asJavaSingle.asInstanceOf[rx.Single[T]]))
  }

  /**
    * Given 3 Singles, returns an single that emits Tuples of 3 elements each.
    * The first emitted Tuple will contain the first element of each source single,
    * the second Tuple the second element of each source single, and so on.
    *
    * @return an Single that emits the zipped Singles
    */
  def zip[A, B, C](obA: Single[A], obB: Single[B], obC: Single[C]): Single[(A, B, C)] = {
    toScalaSingle[(A, B, C)](rx.Single.zip[A, B, C, (A, B, C)](obA.asJavaSingle, obB.asJavaSingle, obC.asJavaSingle, (a: A, b: B, c: C) => (a, b, c)))
  }

  /**
    * Given 4 singles, returns an single that emits Tuples of 4 elements each.
    * The first emitted Tuple will contain the first element of each source observable,
    * the second Tuple the second element of each source single, and so on.
    *
    * @return an Single that emits the zipped Singles
    */
  def zip[A, B, C, D](obA: Single[A], obB: Single[B], obC: Single[C], obD: Single[D]): Single[(A, B, C, D)] = {
    toScalaSingle[(A, B, C, D)](rx.Single.zip[A, B, C, D, (A, B, C, D)](obA.asJavaSingle, obB.asJavaSingle, obC.asJavaSingle, obD.asJavaSingle, (a: A, b: B, c: C, d: D) => (a, b, c, d)))
  }

}



