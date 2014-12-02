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
 * Subject that, once an `Observer` has subscribed, emits all subsequently observed items to the
 * subscriber.
 * <p>
 * <img width="640" height="405" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="" />
 * <p>
 * @example
 {{{
  val subject = PublishSubject[String]()
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1)
  subject.onNext("one")
  subject.onNext("two")
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2)
  subject.onNext("three")
  subject.onCompleted()
  }}}
 */
object PublishSubject {
  /**
   * Creates and returns a new `PublishSubject`.
   *
   * @return the new `PublishSubject`
   */
  def apply[T](): PublishSubject[T] =  new PublishSubject[T](rx.subjects.PublishSubject.create[T]())
}

private [scala] class PublishSubject[T] private [scala] (val asJavaSubject: rx.subjects.PublishSubject[T]) extends Subject[T]  {}

