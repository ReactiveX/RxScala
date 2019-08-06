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
package examples

import java.io.IOException
import org.junit.Assert._
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite
import rx.{Observable => JObservable}
import rx.lang.scala._
import rx.lang.scala.JavaConversions._
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.schedulers.IOScheduler

import scala.concurrent.duration._

class TestSubscriberExample extends JUnitSuite {

  @Test
  def example1(): Unit = {
    val subscriber = TestSubscriber[Int]()
    Observable.just(1, 2, 3).subscribe(subscriber)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertValues(List(1, 2, 3): _*)
    subscriber.assertCompleted()
    subscriber.assertNoErrors()
  }

  @Test
  def example2(): Unit = {
    val subscriber = TestSubscriber[Int]()
    val o = Observable.just(1, 2, 3) ++ Observable.error(new IOException("Oops"))
    o.subscribe(subscriber)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertNotCompleted()
    subscriber.assertError(classOf[IOException])
  }

  @Test
  def example3(): Unit = {
    val subscriber = TestSubscriber[Int]()
    val e = new IOException("Oops")
    val o = Observable.just(1, 2, 3) ++ Observable.error(e)
    o.subscribe(subscriber)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertNotCompleted()
    // Enable it when RxJava 1.0.12 is released because of https://github.com/ReactiveX/RxJava/pull/2999
    // subscriber.assertError(e)
  }

  @Test
  def example4(): Unit = {
    val subscriber = TestSubscriber[Int]()
    Observable.never.subscribe(subscriber)
    subscriber.assertNoValues()
    subscriber.assertNoTerminalEvent()
  }

  @Test
  def example5(): Unit = {
    val subscriber = TestSubscriber[Int]()
    Observable.just(1, 2, 3).subscribeOn(IOScheduler()).subscribe(subscriber)
    subscriber.awaitTerminalEvent(30.seconds)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertCompleted()
    subscriber.assertNoErrors()
    assertTrue(Thread.currentThread() != subscriber.getLastSeenThread)
  }

  @Test
  def example6(): Unit = {
    val subscriber = TestSubscriber[Int]()
    Observable.just(1, 2, 3).subscribe(subscriber)
    subscriber.assertUnsubscribed()
  }

  @Test
  def example7(): Unit = {
    val subscriber = TestSubscriber[Int]()
    Observable.just(1).subscribe(subscriber)
    subscriber.assertValue(1)
  }

  @Test
  def example8(): Unit = {
    val subscriber = TestSubscriber[Int]()
    (0 until 10).toObservable.subscribe(subscriber)
    subscriber.assertValueCount(10)
  }

  @Test
  def example9(): Unit = {
    val subscriber = TestSubscriber[Int]()
    val o = Observable { (subscriber: Subscriber[Int]) =>
      if (!subscriber.isUnsubscribed) {
        subscriber.onNext(1)
      }
      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    }
    o.subscribe(subscriber)
    subscriber.assertValue(1)
    subscriber.assertCompleted()
    subscriber.assertNoErrors()
  }

  @Test
  def example10(): Unit = {
    val subscriber = TestSubscriber[Int](1)
    Observable.just(1, 2, 3).subscribe(subscriber)
    subscriber.assertValues(1)
    subscriber.assertNotCompleted()

    subscriber.requestMore(1)
    subscriber.assertValues(1, 2)
    subscriber.assertNotCompleted()

    subscriber.requestMore(1)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertCompleted()
  }

  @Test
  def example11(): Unit = {
    val subscriber = TestSubscriber[Int]()
    // Use TestSubscriber with RxJava
    JObservable.just(1, 2, 3).subscribe(subscriber)
    subscriber.assertValues(1, 2, 3)
    subscriber.assertCompleted()
    subscriber.assertNoErrors()
  }
}
