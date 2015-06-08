/**
 * Copyright 2015 Netflix, Inc.
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
package rx.lang.scala.observers

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import rx.{Subscriber => JSubscriber, Observer => JObserver, Subscription => JSubscription}
import rx.annotations.Experimental
import rx.observers.{TestSubscriber => JTestSubscriber}
import rx.lang.scala.{Observable, Observer, Subscriber}

/**
 * A [[TestSubscriber]] is a variety of [[Subscriber]] that you can use for unit testing, to perform
 * assertions, inspect received events, or wrap a mocked [[Subscriber]].
 *
 * @define experimental
 * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
 */
class TestSubscriber[T] private[scala](jTestSubscriber: JTestSubscriber[T]) extends Subscriber[T] {

  private[scala] override val asJavaSubscriber: JSubscriber[_ >: T] = jTestSubscriber
  private[scala] override val asJavaObserver: JObserver[_ >: T] = jTestSubscriber
  private[scala] override val asJavaSubscription: JSubscription = jTestSubscriber

  override def onNext(value: T): Unit = jTestSubscriber.onNext(value)

  override def onError(error: Throwable): Unit = jTestSubscriber.onError(error)

  override def onCompleted(): Unit = jTestSubscriber.onCompleted()

  /**
   * Get the `Throwable`s this [[Subscriber]] was notified of via [[onError]]
   *
   * @return a sequence of the `Throwable`s that were passed to the [[Subscriber.onError]] method
   */
  def getOnErrorEvents: Seq[Throwable] = {
    jTestSubscriber.getOnErrorEvents()
  }

  /**
   * Get the sequence of items observed by this [[Subscriber]].
   *
   * @return a sequence of items observed by this [[Subscriber]], in the order in which they were observed
   */
  def getOnNextEvents: Seq[T] = {
    jTestSubscriber.getOnNextEvents()
  }

  /**
   * Allow calling the protected [[request]] from unit tests.
   *
   * @param n the maximum number of items you want the Observable to emit to the Subscriber at this time, or
   *          `Long.MaxValue` if you want the Observable to emit items at its own pace
   */
  def requestMore(n: Long): Unit = {
    jTestSubscriber.requestMore(n)
  }

  /**
   * Assert that a single terminal event occurred, either `onCompleted` or `onError`.
   *
   * @throws AssertionError if not exactly one terminal event notification was received
   */
  @throws[AssertionError]
  def assertTerminalEvent(): Unit = {
    jTestSubscriber.assertTerminalEvent()
  }

  /**
   * Assert that this [[Subscriber]] is unsubscribed.
   *
   * @throws AssertionError if this [[Subscriber]] is not unsubscribed
   */
  @throws[AssertionError]
  def assertUnsubscribed(): Unit = {
    jTestSubscriber.assertUnsubscribed()
  }

  /**
   * Assert that this [[Subscriber]] has received no `onError` notifications.
   *
   * @throws AssertionError if this [[Subscriber]] has received one or more `onError` notifications
   */
  @throws[AssertionError]
  def assertNoErrors(): Unit = {
    jTestSubscriber.assertNoErrors()
  }

  /**
   * Blocks until this [[Subscriber]] receives a notification that the [[Observable]] is complete
   * (either an `onCompleted` or `onError` notification).
   *
   * @throws RuntimeException if the Subscriber is interrupted before the Observable is able to complete
   */
  @throws[RuntimeException]
  def awaitTerminalEvent(): Unit = {
    jTestSubscriber.awaitTerminalEvent()
  }

  /**
   * Blocks until this [[Subscriber]] receives a notification that the [[Observable]] is complete
   * (either an `onCompleted` or `onError` notification), or until a timeout expires.
   *
   * @param timeout the duration of the timeout
   * @throws RuntimeException if the Subscriber is interrupted before the Observable is able to complete
   */
  @throws[RuntimeException]
  def awaitTerminalEvent(timeout: Duration): Unit = {
    jTestSubscriber.awaitTerminalEvent(timeout.toNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Blocks until this [[Subscriber]] receives a notification that the [[Observable]] is complete
   * (either an `onCompleted` or `onError` notification), or until a timeout expires; if the
   * [[Subscriber]] is interrupted before either of these events take place, this method unsubscribes the
   * [[Subscriber]] from the [[Observable]]).
   *
   * @param timeout the duration of the timeout
   */
  def awaitTerminalEventAndUnsubscribeOnTimeout(timeout: Duration): Unit = {
    jTestSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(timeout.toNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Returns the last thread that was in use when an item or notification was received by this [[Subscriber]].
   *
   * @return the `Thread` on which this [[Subscriber]] last received an item or notification from the
   *         [[Observable]] it is subscribed to
   */
  def getLastSeenThread: Thread = {
    jTestSubscriber.getLastSeenThread
  }

  /**
   * $experimental Assert if there is exactly a single completion event.
   *
   * @throws AssertionError if there were zero, or more than one, onCompleted events
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertCompleted(): Unit = {
    jTestSubscriber.assertCompleted()
  }

  /**
   * $experimental Assert if there is no completion event.
   *
   * @throws AssertionError if there were one or more than one onCompleted events
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertNotCompleted(): Unit = {
    jTestSubscriber.assertNotCompleted()
  }

  /**
   * $experimental Assert if there is exactly one error event which is a subclass of the given class.
   *
   * @param clazz the class to check the error against.
   * @throws AssertionError if there were zero, or more than one, onError events, or if the single onError
   *                        event did not carry an error of a subclass of the given class
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertError(clazz: Class[_ <: Throwable]): Unit = {
    jTestSubscriber.assertError(clazz)
  }

  /**
   * $experimental Assert there is a single onError event with the exact exception.
   *
   * @param throwable the throwable to check
   * @throws AssertionError if there were zero, or more than one, onError events, or if the single onError
   *                        event did not carry an error that matches the specified throwable
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertError(throwable: Throwable): Unit = {
    jTestSubscriber.assertError(throwable)
  }

  /**
   * $experimental Assert for no onError and onCompleted events.
   *
   * @throws AssertionError if there was either an onError or onCompleted event
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertNoTerminalEvent(): Unit = {
    jTestSubscriber.assertNoTerminalEvent()
  }

  /**
   * $experimental Assert if there are no onNext events received.
   *
   * @throws AssertionError if there were any onNext events
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertNoValues(): Unit = {
    jTestSubscriber.assertNoValues()
  }

  /**
   * $experimental Assert if the given number of onNext events are received.
   *
   * @param count the expected number of onNext events
   * @throws AssertionError if there were more or fewer onNext events than specified by `count`
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertValueCount(count: Int): Unit = {
    jTestSubscriber.assertValueCount(count)
  }

  /**
   * $experimental Assert if the received onNext events, in order, are the specified items.
   *
   * @param values the items to check
   * @throws AssertionError if the items emitted do not exactly match those specified by `values`
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertValues(values: T*): Unit = {
    jTestSubscriber.assertValues(values: _*)
  }

  /**
   * $experimental Assert if there is only a single received onNext event and that it marks the emission of a specific item.
   *
   * @param value the item to check
   * @throws AssertionError if the [[Observable]] does not emit only the single item specified by `value`
   * @since (if this graduates from "Experimental" replace this parenthetical with the release number)
   */
  @Experimental
  @throws[AssertionError]
  def assertValue(value: T): Unit = {
    jTestSubscriber.assertValue(value)
  }
}

/**
 * @define experimental
 * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
 */
object TestSubscriber {

  def apply[T](): TestSubscriber[T] =
    new TestSubscriber(new JTestSubscriber[T]())

  def apply[T](delegate: Observer[T]): TestSubscriber[T] =
    new TestSubscriber(new JTestSubscriber[T](delegate.asJavaObserver.asInstanceOf[JObserver[T]]))


  def apply[T](delegate: Subscriber[T]): TestSubscriber[T] =
    new TestSubscriber(new JTestSubscriber[T](delegate.asJavaSubscriber.asInstanceOf[JSubscriber[T]]))

  /**
   * $experimental Constructs a [[TestSubscriber]] with the initial request to be requested from upstream.
   * @param initialRequest the initial request value, negative value will revert to the default unbounded behavior
   */
  @Experimental
  def apply[T](initialRequest: Long): TestSubscriber[T] = {
    new TestSubscriber(new JTestSubscriber[T](initialRequest))
  }

  /**
   * Constructs a [[TestSubscriber]] with the initial request to be requested from upstream and a delegate [[Observer]] to wrap.
   * @param initialRequest the initial request value, negative value will revert to the default unbounded behavior
   * @param delegate the Observer instance to wrap
   */
  @Experimental
  def apply[T](delegate: Observer[T], initialRequest: Long): TestSubscriber[T] = {
    new TestSubscriber(new JTestSubscriber[T](delegate.asJavaObserver.asInstanceOf[JObserver[T]], initialRequest))
  }
}
