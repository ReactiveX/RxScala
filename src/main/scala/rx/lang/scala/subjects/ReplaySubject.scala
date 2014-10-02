/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala.subjects

import rx.lang.scala.{Scheduler, Subject}

import scala.concurrent.duration.Duration

/**
 * Subject that buffers all items it observes and replays them to any `Observer` that subscribes.
 * <p>
 * <img width="640" height="405" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.ReplaySubject.png" alt="">
 * <p>
 * Example usage:
 * {{{
  val subject = ReplaySubject[String]()
  subject.onNext("one")
  subject.onNext("two")
  subject.onNext("three")
  subject.onCompleted()

  // both of the following will get the onNext/onCompleted calls from above
  subject.subscribe(observer1)
  subject.subscribe(observer2)
  }}}
 */
object ReplaySubject {
  /**
   * Creates an unbounded replay subject.
   * <p>
   * The internal buffer is backed by an `ArrayList` and starts with an initial capacity of 16. Once the
   * number of items reaches this capacity, it will grow as necessary (usually by 50%). However, as the
   * number of items grows, this causes frequent array reallocation and copying, and may hurt performance
   * and latency. This can be avoided with the `apply(Int)` overload which takes an initial capacity
   * parameter and can be tuned to reduce the array reallocation frequency as needed.
   *
   * @return the created subject
   */
  def apply[T](): ReplaySubject[T] = {
    new ReplaySubject[T](rx.subjects.ReplaySubject.create())
  }

  /**
   * Creates an unbounded replay subject with the specified initial buffer capacity.
   * <p>
   * Use this method to avoid excessive array reallocation while the internal buffer grows to accomodate new
   * items. For example, if you know that the buffer will hold 32k items, you can ask the
   * `ReplaySubject` to preallocate its internal array with a capacity to hold that many items. Once
   * the items start to arrive, the internal array won't need to grow, creating less garbage and no overhead
   * due to frequent array-copying.
   *
   * @param capacity the initial buffer capacity
   * @return the created subject
   */
  def apply[T](capacity: Int): ReplaySubject[T] = {
    new ReplaySubject[T](rx.subjects.ReplaySubject.create(capacity))
  }

  /**
   * Creates a size-bounded replay subject.
   * <p>
   * In this setting, the `ReplaySubject` holds at most `size` items in its internal buffer and
   * discards the oldest item.
   * <p>
   * When observers subscribe to a terminated `ReplaySubject`, they are guaranteed to see at most
   * `size` `onNext` events followed by a termination event.
   * <p>
   * If an observer subscribes while the `ReplaySubject` is active, it will observe all items in the
   * buffer at that point in time and each item observed afterwards, even if the buffer evicts items due to
   * the size constraint in the mean time. In other words, once an Observer subscribes, it will receive items
   * without gaps in the sequence.
   *
   * @param size the maximum number of buffered items
   * @return the created subject
   */
  def withSize[T](size: Int): ReplaySubject[T] = {
    new ReplaySubject[T](rx.subjects.ReplaySubject.createWithSize(size))
  }

  /**
   * Creates a time-bounded replay subject.
   * <p>
   * In this setting, the `ReplaySubject` internally tags each observed item with a timestamp value
   * supplied by the `Scheduler` and keeps only those whose age is less than the supplied time value
   * converted to milliseconds. For example, an item arrives at T=0 and the max age is set to 5; at T&gt;=5
   * this first item is then evicted by any subsequent item or termination event, leaving the buffer empty.
   * <p>
   * Once the subject is terminated, observers subscribing to it will receive items that remained in the
   * buffer after the terminal event, regardless of their age.
   * <p>
   * If an observer subscribes while the `ReplaySubject` is active, it will observe only those items
   * from within the buffer that have an age less than the specified time, and each item observed thereafter,
   * even if the buffer evicts items due to the time constraint in the mean time. In other words, once an
   * observer subscribes, it observes items without gaps in the sequence except for any outdated items at the
   * beginning of the sequence.
   * <p>
   * Note that terminal notifications `onError` and `onCompleted` trigger eviction as well. For
   * example, with a max age of 5, the first item is observed at T=0, then an `onCompleted` notification
   * arrives at T=10. If an observer subscribes at T=11, it will find an empty `ReplaySubject` with just
   * an `onCompleted` notification.
   *
   * @param time the maximum age of the contained items
   * @param scheduler the `Scheduler` that provides the current time
   * @return the created subject
   */
  def withTime[T](time: Duration, scheduler: Scheduler): ReplaySubject[T] = {
    new ReplaySubject[T](rx.subjects.ReplaySubject.createWithTime(time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Creates a time- and size-bounded replay subject.
   * <p>
   * In this setting, the `ReplaySubject` internally tags each received item with a timestamp value
   * supplied by the `Scheduler` and holds at most `size` items in its internal buffer. It evicts
   * items from the start of the buffer if their age becomes less-than or equal to the supplied age in
   * milliseconds or the buffer reaches its `size` limit.
   * <p>
   * When observers subscribe to a terminated `ReplaySubject`, they observe the items that remained in
   * the buffer after the terminal notification, regardless of their age, but at most `size` items.
   * <p>
   * If an observer subscribes while the `ReplaySubject` is active, it will observe only those items
   * from within the buffer that have age less than the specified time and each subsequent item, even if the
   * buffer evicts items due to the time constraint in the mean time. In other words, once an observer
   * subscribes, it observes items without gaps in the sequence except for the outdated items at the beginning
   * of the sequence.
   * <p>
   * Note that terminal notifications (`onError` and `onCompleted`) trigger eviction as well. For
   * example, with a max age of 5, the first item is observed at T=0, then an `onCompleted` notification
   * arrives at T=10. If an observer subscribes at T=11, it will find an empty `ReplaySubject` with just
   * an `onCompleted` notification.
   *
   * @param time the maximum age of the contained items
   * @param size the maximum number of buffered items
   * @param scheduler the `Scheduler` that provides the current time
   * @return the created subject
   */
  def withTimeAndSize[T](time: Duration, size: Int, scheduler: Scheduler): ReplaySubject[T] = {
    new ReplaySubject[T](rx.subjects.ReplaySubject.createWithTimeAndSize(time.length, time.unit, size, scheduler.asJavaScheduler))
  }
}

class ReplaySubject[T] private[scala] (val asJavaSubject: rx.subjects.ReplaySubject[T]) extends Subject[T]  {}
