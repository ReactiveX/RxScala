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
package rx.lang.scala

import rx.annotations.Experimental
import rx.lang.scala.subjects.SerializedSubject

/**
* A Subject is an Observable and an Observer at the same time.
 *
 * @define experimental
 * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
 *
 * @define beta
 * <span class="badge badge-red" style="float: right;">BETA</span>
*/
trait Subject[T] extends Observable[T] with Observer[T] {
  private [scala] val asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]

  val asJavaObservable: rx.Observable[_ <: T] = asJavaSubject

  override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubject
  override def onNext(value: T): Unit = { asJavaObserver.onNext(value)}
  override def onError(error: Throwable): Unit = { asJavaObserver.onError(error)  }
  override def onCompleted() { asJavaObserver.onCompleted() }

  /**
   * Indicates whether the [[Subject]] has [[Observer]]s subscribed to it.
   * @return `true` if there is at least one [[Observer]] subscribed to this [[Subject]], `false` otherwise
   */
  def hasObservers: Boolean = asJavaSubject.hasObservers

  /**
   * Wraps a [[Subject]] so that it is safe to call its various `on` methods from different threads.
   *
   * When you use an ordinary [[Subject]] as a [[Subscriber]], you must take care not to call its
   * [[Subscriber.onNext]] method (or its other `on` methods) from multiple threads, as this could
   * lead to non-serialized calls, which violates the [[Observable]] contract and creates an ambiguity
   * in the resulting [[Subject]].
   *
   * To protect a [[Subject]] from this danger, you can convert it into a [[rx.lang.scala.subjects.SerializedSubject SerializedSubject]]
   * with code like the following:
   * {{{
   * mySafeSubject = myUnsafeSubject.toSerialized
   * }}}
   *
   * @return [[rx.lang.scala.subjects.SerializedSubject SerializedSubject]] wrapping the current [[Subject]]
   */
  def toSerialized: SerializedSubject[T] = this match {
    case s: SerializedSubject[T] => s
    case s => SerializedSubject(s)
  }

  /**
   * $experimental Check if the Subject has terminated with an exception.
   * <p>The operation is threadsafe.
   *
   * @return `true` if the subject has received a throwable through { @code onError}.
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def hasThrowable: Boolean = asJavaSubject.hasThrowable

  /**
   * $experimental Check if the Subject has terminated normally.
   * <p>The operation is threadsafe.
   *
   * @return `true` if the subject completed normally via { @code onCompleted}
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def hasCompleted: Boolean = asJavaSubject.hasCompleted

  /**
   * $experimental Returns the Throwable that terminated the Subject.
   * <p>The operation is threadsafe.
   *
   * @return the Throwable that terminated the Subject or { @code null} if the subject hasn't terminated yet or
   *                                                              if it terminated normally.
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def getThrowable: Throwable = asJavaSubject.getThrowable

  /**
   * $experimental Check if the Subject has any value.
   * <p>Use the `#getValue()` method to retrieve such a value.
   * <p>Note that unless `#hasCompleted()` or `#hasThrowable()` returns true, the value
   * retrieved by `getValue()` may get outdated.
   * <p>The operation is threadsafe.
   *
   * @return { @code true} if and only if the subject has some value but not an error
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def hasValue: Boolean = asJavaSubject.hasValue

  /**
   * $experimental Returns the current or latest value of the Subject if there is such a value and
   * the subject hasn't terminated with an exception.
   * <p>The method can return `null` for various reasons. Use `#hasValue()`, `#hasThrowable()`
   * and `#hasCompleted()` to determine if such `null` is a valid value, there was an
   * exception or the Subject terminated without receiving any value.
   * <p>The operation is threadsafe.
   *
   * @return the current value or { @code null} if the Subject doesn't have a value, has terminated with an
   *                                      exception or has an actual { @code null} as a value.
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def getValue: T = asJavaSubject.getValue.asInstanceOf[T]

  /**
   * $experimental Returns a snapshot of the currently buffered non-terminal events.
   * <p>The operation is threadsafe.
   *
   * @return a snapshot of the currently buffered non-terminal events.
   * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
   */
  @Experimental
  def getValues: Seq[AnyRef] = asJavaSubject.getValues
}

/**
 * Subject that, once an `Observer` has subscribed, emits all subsequently observed items to the
 * subscriber.
 * <p>
 * <img width="640" height="405" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="" />
 * <p>
 * @example
 {{{
  val subject = Subject[String]()
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
object Subject {
  /**
   * Creates and returns a new `Subject`.
   *
   * @return the new `Subject`
   */
  def apply[T](): Subject[T] = new rx.lang.scala.subjects.PublishSubject[T](rx.subjects.PublishSubject.create())
}

