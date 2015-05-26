/**
 * Copyright 2014 Netflix, Inc.
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

import scala.language.implicitConversions
import rx.functions._
import JavaConversions._

/**
 * @define noDefaultScheduler
 * ===Scheduler:===
 * This method does not operate by default on a particular [[Scheduler]].
 *
 * A class to add `Experimental/Beta` APIs in RxJava. These APIs can change at any time.
 *
 * `import rx.lang.scala.ExperimentalAPIs._` to enable them.
 */
@deprecated("Use new methods in [[Observable]] instead. This is kept here only for backward compatibility.", "0.25.0")
class ExperimentalObservable[+T](private val o: Observable[T]) {

  /**
   * Instructs an [[Observable]] will block the producer thread if the source emits items faster than its [[Observer]] can consume them
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
   */
  @deprecated("Use [[[Observable.onBackpressureBlock(maxQueueLength:Int)*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureBlock(maxQueueLength: Int): Observable[T] = {
    o.asJavaObservable.onBackpressureBlock(maxQueueLength)
  }

  /**
   * Instructs an [[Observable]] will block the producer thread if the source emits items faster than its [[Observer]] can consume them
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
   */
  @deprecated("Use [[[Observable.onBackpressureBlock:*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureBlock: Observable[T] = {
    o.asJavaObservable.onBackpressureBlock()
  }

  /**
   * An [[Observable]] wrapping the source one that will invokes the given action when it receives a request for more items.
   *
   * $noDefaultScheduler
   *
   * @param onRequest the action that gets called when an [[Observer]] requests items from this [[Observable]]
   * @return an [[Observable]] that will call `onRequest` when appropriate
   * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
   */
  @deprecated("Use [[Observable.doOnRequest]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def doOnRequest(onRequest: Long => Unit): Observable[T] = {
    o.asJavaObservable.doOnRequest(new Action1[java.lang.Long] {
      override def call(request: java.lang.Long): Unit = onRequest(request)
    })
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
    // Use `onBackpressureBufferWithCapacity` because if not, it will conflict with `Observable.onBackpressureBuffer`
    o.asJavaObservable.onBackpressureBuffer(capacity)
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
    // Use `onBackpressureBufferWithCapacity` because if not, it will conflict with `Observable.onBackpressureBuffer`
    o.asJavaObservable.onBackpressureBuffer(capacity, new Action0 {
      override def call(): Unit = onOverflow
    })
  }

  /**
   * Returns an [[Observable]] that emits the items emitted by the source [[Observable]] or the items of an alternate
   * [[Observable]] if the source [[Observable]] is empty.
   *
   * $noDefaultScheduler
   *
   * @param alternate the alternate [[Observable]] to subscribe to if the source does not emit any items
   * @return an [[Observable]] that emits the items emitted by the source [[Observable]] or the items of an
   *         alternate [[Observable]] if the source [[Observable]] is empty.
   */
  @deprecated("Use [[Observable.switchIfEmpty]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def switchIfEmpty[U >: T](alternate: Observable[U]): Observable[U] = {
    val jo = o.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](jo.switchIfEmpty(alternate.asJavaObservable))
  }

  /**
   * Merges the specified [[Observable]] into this [[Observable]] sequence by using the `resultSelector`
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
   */
  @deprecated("Use [[Observable.withLatestFrom]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def withLatestFrom[U, R](other: Observable[U])(resultSelector: (T, U) => R): Observable[R] = {
    val func = new Func2[T, U, R] {
      override def call(t1: T, t2: U): R = resultSelector(t1, t2)
    }
    toScalaObservable[R](o.asJavaObservable.withLatestFrom(other.asJavaObservable, func))
  }

  /**
   * Returns an [[Observable]] that emits items emitted by the source [[Observable]], checks the specified predicate
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
   */
  @deprecated("Use [[[Observable.takeUntil(stopPredicate:T=>Boolean)*]]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def takeUntil(stopPredicate: T => Boolean): Observable[T] = {
    val func = new Func1[T, java.lang.Boolean] {
      override def call(t: T): java.lang.Boolean = stopPredicate(t)
    }
    toScalaObservable[T](o.asJavaObservable.takeUntil(func))
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
  @deprecated("This is kept here only for backward compatibility.", "0.25.0")
  def flatMapWithMaxConcurrent[R](f: T => Observable[R], maxConcurrent: Int): Observable[R] = {
    toScalaObservable[R](o.asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]] {
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
  @deprecated("This is kept here only for backward compatibility.", "0.25.0")
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
    toScalaObservable[R](o.asJavaObservable.flatMap[R](jOnNext, jOnError, jOnCompleted, maxConcurrent))
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
  @deprecated("Use [[Observable.onBackpressureDrop(onDrop:T=>Unit)*]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def onBackpressureDropDo(onDrop: T => Unit): Observable[T] = {
    toScalaObservable[T](o.asJavaObservable.onBackpressureDrop(new Action1[T] {
      override def call(t: T) = onDrop(t)
    }))
  }
}

@deprecated("Use new methods in [[Observable]] instead. This is kept here only for backward compatibility.", "0.25.0")
object ExperimentalAPIs {
  @deprecated("Use new methods in [[Observable]] instead. This is kept here only for backward compatibility.", "0.25.0")
  def toExperimentalObservable[T](o: Observable[T]): ExperimentalObservable[T] = new ExperimentalObservable(o)
}
