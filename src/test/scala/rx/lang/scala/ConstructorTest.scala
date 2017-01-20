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
import scala.language.postfixOps
import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import rx.Emitter.BackpressureMode
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.subjects.PublishSubject

import scala.concurrent.duration._

class ConstructorTest extends JUnitSuite {

  @Test def toObservable() {
    val xs = List(1,2,3).toObservable.toBlocking.toList
    assertEquals(List(1,2,3), xs)

    val ys = Observable.from(List(1,2,3)).toBlocking.toList
    assertEquals(List(1,2,3), xs)

    val zs = Observable.just(1,2,3).toBlocking.toList
    assertEquals(List(1,2,3), xs)

  }

  @Test def testFromEmitter(): Unit = {
    val max = 200

    trait Callback {
      def newValue(v: Int): Unit
      def complete(): Unit
    }

    def callbackStyleAPI(callback: Callback): Unit = {
      new Thread(new Runnable {
        override def run(): Unit = {
          Range(0, max).foreach(callback.newValue)
          callback.complete()
        }
      }).start()
    }

    val o = Observable.fromEmitter[Int](e => {
      callbackStyleAPI(new Callback {
        override def newValue(v: Int): Unit = e.onNext(v)
        override def complete(): Unit = e.onCompleted()
      })
    }, BackpressureMode.BUFFER)

    assertEquals(Range(0,max).toList, o.toBlocking.toList)
  }

  @Test def testFromEmitterError(): Unit = {
    val ex = new RuntimeException("Oopsie")

    trait Callback {
      def error(e: Throwable): Unit
    }

    def callbackStyleAPI(callback: Callback): Unit = {
      new Thread(new Runnable {
        override def run(): Unit = callback.error(ex)
      }).start()
    }

    val o = Observable.fromEmitter[Int](e => {
      callbackStyleAPI(new Callback {
        override def error(err: Throwable) = e.onError(err)
      })
    }, BackpressureMode.BUFFER)

    val testSubscriber = TestSubscriber[Int]()
    o.subscribe(testSubscriber)
    testSubscriber.awaitTerminalEvent(500.millis)
    testSubscriber.assertError(ex)
  }

  @Test def testFromEmitterCancel(): Unit = {
    val stopSubject = Subject[Unit]()
    val testSubscriber = TestSubscriber[Unit]()
    stopSubject.subscribe(testSubscriber)

    trait Callback {
      def newValue(v: Int): Unit
    }

    def callbackStyleAPI(callback: Callback): () => Unit = {
      @volatile var stop = false
      val t = new Thread(new Runnable {
        override def run(): Unit = {
          var i = 0
          while (!stop) {
            callback.newValue(i)
            i += 1
          }
          stopSubject.onNext(())
          stopSubject.onCompleted()
        }
      })
      t.start()
      () => { stop = true }
    }


    val values = Observable.fromEmitter[Int](e => {
      val stopF = callbackStyleAPI(new Callback {
        override def newValue(v: Int) = e.onNext(v)
      })
      e.setCancellation(stopF)
    },BackpressureMode.DROP)

    assertEquals(200, values.take(200).toBlocking.toList.size)
    testSubscriber.awaitTerminalEvent(100.millis)
    testSubscriber.assertValue(())
  }
}
