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


import org.junit.Test
import org.junit.Assert._
import org.scalatestplus.junit.JUnitSuite
import rx.lang.scala.Notification.{OnCompleted, OnError, OnNext}


class NotificationTests extends JUnitSuite {
  @Test
  def creation(): Unit = {

    val onNext = OnNext(42)
      assertEquals(42, onNext match { case OnNext(value) => value })

    val oops = new Exception("Oops")
    val onError = OnError(oops)
      assertEquals(oops, onError match { case OnError(error) => error })

    val onCompleted = OnCompleted
      assertEquals((), onCompleted match { case OnCompleted => () })
  }

  @Test
  def accept(): Unit = {

    val onNext = OnNext(42)
      assertEquals(42, onNext(x=>42, e=>4711,()=>13))

    val oops = new Exception("Oops")
    val onError = OnError(oops)
      assertEquals(4711, onError(x=>42, e=>4711,()=>13))

    val onCompleted = OnCompleted
      assertEquals(13, onCompleted(x=>42, e=>4711,()=>13))

  }

  @Test
  def testFlatMapNextToNext(): Unit = {
    val notification = OnNext(41).flatMap(i => Notification.OnNext(i+1))
    assertEquals(42, notification(i=>i, _ => -1, () => -1))
  }

  @Test
  def testFlatMapNextToError(): Unit = {
    val oops = new Exception("Oops")
    val notification = OnNext(()).flatMap(_ => Notification.OnError(oops))
    assertEquals(42, notification(_ => -1, {
      case `oops` => 42
      case _ => -1
    }, () => -1))
  }

  @Test
  def testFlatMapNextToCompletion(): Unit = {
    val notification = OnNext(()).flatMap(_ => Notification.OnCompleted)
    assertEquals(42, notification(_ => -1, e => -1, () => 42))
  }

  @Test
  def testFlatMapCompleted(): Unit = {
    val notification = OnCompleted.flatMap(_ => Notification.OnNext(1))
    assertEquals(42, notification(_ => -1, e => -1, () => 42))
  }

  @Test
  def testFlatMapError(): Unit = {
    val oops = new Exception("Oops")
    val notification = OnError(oops).flatMap(_ => Notification.OnNext(1))
    assertEquals(42, notification(_ => -1, {
      case `oops` => 42
      case _ => -1
    }, () => -1))
  }

  @Test
  def map(): Unit = {
    val notification = OnNext(41).map(_+1)
    assertEquals(42, notification(i=>i, _ => -1, () => -1))
  }
}
