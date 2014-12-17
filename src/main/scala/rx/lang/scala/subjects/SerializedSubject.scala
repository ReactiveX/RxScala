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
package rx.lang.scala.subjects

import rx.lang.scala.Subject

/**
 * Wraps a Subject to ensure that the resulting Subject is chronologically well-behaved.
 * 
 * A well-behaved Subject does not interleave its invocations of the [[rx.lang.scala.Subject.onNext onNext]],
 * [[rx.lang.scala.Subject.onCompleted onCompleted]], and [[rx.lang.scala.Subject.onError onError]] methods of
 * its [[rx.lang.scala.Subject]]s; it invokes `onCompleted` or `onError` only once; and it never invokes `onNext`
 * after invoking either `onCompleted` or `onError`.
 * 
 * [[rx.lang.scala.subjects.SerializedSubject]] enforces this, and the Subject it returns invokes `onNext` and
 * `onCompleted` or `onError` synchronously on the wrapped Subject.
 */
object SerializedSubject {
  /**
   * Creates and returns a new `SerializedSubject`.
   *
   * @param actual actual `Subject` to wrap and synchronously notify
   * @return a `SerializedSubject` that is a chronologically well-behaved version of the actual
   *         Subject, and that synchronously notifies the wrapped `Subject`
   * @see RxScalaDemo.eventBusExample for example usage
   */
  def apply[T](actual: Subject[T]): SerializedSubject[T] = {
    val jSubject = actual.asJavaSubject.toSerialized
    new SerializedSubject[T](jSubject)
  }
}

private [scala] class SerializedSubject[T] private [scala] (val asJavaSubject: rx.subjects.SerializedSubject[_ >: T, _ <: T]) extends Subject[T]
