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
import rx.lang.scala.schedulers.TestScheduler

/**
 * A variety of Subject that is useful for testing purposes. It operates on a `TestScheduler` and allows
 * you to precisely time emissions and notifications to the Subject's subscribers.
 */
object TestSubject {
  /**
   * Creates and returns a new `TestSubject`.
   *
   * @param scheduler a `TestScheduler` on which to operate this Subject
   * @return the new `TestSubject`
   */
  def apply[T](scheduler: TestScheduler): TestSubject[T] = {
    new TestSubject[T](rx.subjects.TestSubject.create(scheduler.asJavaScheduler))
  }
}

class TestSubject[T] private[scala] (val asJavaSubject: rx.subjects.TestSubject[T]) extends Subject[T]  {}
