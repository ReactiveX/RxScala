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

import rx.lang.scala.Subject

/**
 * Subject that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * `Observer`.
 * <p>
 * <img width="640" height="405" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorSubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre>
  // observer will receive all events.
  val subject = BehaviorSubject[String]("default")
  subject.subscribe(observer)
  subject.onNext("one")
  subject.onNext("two")
  subject.onNext("three")

  // observer will receive the "one", "two" and "three" events, but not "zero"
  val subject = BehaviorSubject[String]("default")
  subject.onNext("zero")
  subject.onNext("one")
  subject.subscribe(observer)
  subject.onNext("two")
  subject.onNext("three")

  // observer will receive only onCompleted
  val subject = BehaviorSubject[String]("default")
  subject.onNext("zero")
  subject.onNext("one")
  subject.onCompleted()
  subject.subscribe(observer)

  // observer will receive only onError
  val subject = BehaviorSubject[String]("default")
  subject.onNext("zero")
  subject.onNext("one")
  subject.onError(new RuntimeException("error"))
  subject.subscribe(observer)
  </pre>
 */
object BehaviorSubject {
  /**
   * Creates a `BehaviorSubject` without a default item.
   *
   * @return the constructed `BehaviorSubject`
   */
  def apply[T](): BehaviorSubject[T] = {
    new BehaviorSubject[T](rx.subjects.BehaviorSubject.create())
  }

  /**
   * Creates a `BehaviorSubject` that emits the last item it observed and all subsequent items to each
   * `Observer` that subscribes to it.
   *
   * @param defaultValue the item that will be emitted first to any `Observer` as long as the
   * `BehaviorSubject` has not yet observed any items from its source `Observable`
   * @return the constructed `BehaviorSubject`
   */
  def apply[T](defaultValue: T): BehaviorSubject[T] = {
    new BehaviorSubject[T](rx.subjects.BehaviorSubject.create(defaultValue))
  }
}

class BehaviorSubject[T] private[scala] (val asJavaSubject: rx.subjects.BehaviorSubject[T]) extends Subject[T]  {}
