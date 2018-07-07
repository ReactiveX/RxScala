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

import rx.lang.scala.observers.TestSubscriber

import scala.collection.mutable

import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.scalatest.junit.JUnitSuite

class SubscriberTests extends JUnitSuite {

  @Test def testIssue1173(): Unit = {
    // https://github.com/Netflix/RxJava/issues/1173
    val subscriber = Subscriber((n: Int) => println(n))
    assertNotNull(subscriber.asJavaObserver)
    assertNotNull(subscriber.asJavaSubscription)
    assertNotNull(subscriber.asJavaSubscriber)
  }

  @Test def testUnsubscribeForSubscriber(): Unit = {
    var innerSubscriber: Subscriber[Int] = null
    val o = Observable[Int](subscriber => {
      Observable[Int](subscriber => {
        innerSubscriber = subscriber
      }).subscribe(subscriber)
    })
    o.subscribe().unsubscribe()
    // If we unsubscribe outside, the inner Subscriber should also be unsubscribed
    assertTrue(innerSubscriber.isUnsubscribed)
  }

  @Test def testBlockCallbackOnlyOnce(): Unit = {
    var called = false
    val o = Observable[Int](subscriber => {
      subscriber.add({ called = !called })
    })

    val subscription = o.subscribe()
    subscription.unsubscribe()
    subscription.unsubscribe()

    // Even if called multiple times, callback is only called once
    assertTrue(called)
    assertTrue(subscription.isUnsubscribed)
  }

  @Test def testNewSubscriber(): Unit = {
    var didComplete = false
    var didError = false
    var onNextValue = 0

    Observable.just(1).subscribe(new Subscriber[Int] {
      override def onCompleted(): Unit = {
        didComplete = true
      }

      override def onError(e: Throwable): Unit = {
        didError = true
      }

      override def onNext(v: Int): Unit = {
        onNextValue = v
      }
    })

    assertTrue("Subscriber called onCompleted", didComplete)
    assertFalse("Subscriber did not call onError", didError)
    assertEquals(1, onNextValue)
  }

  @Test def testOnStart(): Unit = {
    var called = false
    Observable.just(1).subscribe(new Subscriber[Int] {
      override def onStart(): Unit = {
        called = true
      }

      override def onCompleted(): Unit = {
      }

      override def onError(e: Throwable): Unit = {
      }

      override def onNext(v: Int): Unit = {
      }
    })
    assertTrue("Subscriber.onStart should be called", called)
  }

  @Test def testOnStart2(): Unit = {
    val items = scala.collection.mutable.ListBuffer[Int]()
    var calledOnCompleted = false
    Observable.just(1, 2, 3).subscribe(new Subscriber[Int] {
      override def onStart(): Unit = {
        request(1)
      }

      override def onCompleted(): Unit = {
        calledOnCompleted = true
      }

      override def onError(e: Throwable): Unit = {
      }

      override def onNext(v: Int): Unit = {
        items += v
        request(1)
      }
    })
    assertEquals(List(1, 2, 3), items)
    assertTrue("Subscriber.onCompleted should be called", calledOnCompleted)
  }

  @Test def testApplyWithRxSubscriber(): Unit = {
     val l = mutable.ListBuffer[Int]()
     var completed = false

    // A special rx.Subscriber to make sure backpressure works
     val rxSubscriber = new rx.Subscriber[Int]() {
       override def onStart(): Unit = {
         request(1)
       }

       override def onNext(v: Int): Unit = {
         l += v
         request(1)
       }

       override def onError(e: Throwable): Unit = {
         e.printStackTrace()
       }

       override def onCompleted(): Unit = {
         completed = true
       }
     }

    // A special Observable to make sure backpressure works
    var requestOne = true
    Observable {
      (subscriber: Subscriber[Int]) => {
        var count = 0
        subscriber.setProducer(
          n => {
            if (count < 10) {
              if (n != 1) {
                requestOne = false
                subscriber.onError(new AssertionError("backpressure does not work"))
              }
              else {
                count += 1
                subscriber.onNext(0)
                if (count == 10) {
                  subscriber.onCompleted()
                }
              }
            }
        })
      }
    }.subscribe(Subscriber(rxSubscriber))

    assertTrue("backpressure does not work", requestOne)
    assertTrue("onCompleted isn't called", completed)
    assertTrue("onCompleted isn't called", completed)
    val zeros = new Array[Int](10).toList
    assertEquals(zeros, l)
  }

  @Test def testIssue202(): Unit = {
    // https://github.com/ReactiveX/RxScala/issues/202
    val subject = Subject[Option[Unit]]()
    val testSubscriber = TestSubscriber[Option[Unit]]()
    subject.filter(_.isDefined).subscribe(testSubscriber)
    testSubscriber.assertNoValues()
    subject.onNext(None)
    testSubscriber.assertNoValues()
    subject.onNext(Some(()))
    testSubscriber.assertValuesAndClear(Some(()))
    testSubscriber.assertNoValues()
    subject.onNext(None)
    testSubscriber.assertNoValues()
  }
}
