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


import org.junit.Assert.{assertEquals, assertFalse, assertTrue}
import org.junit.{Assert, Test}
import org.scalatestplus.junit.JUnitSuite
import rx.lang.scala.subjects.{AsyncSubject, BehaviorSubject, ReplaySubject, UnicastSubject}

class SubjectTest extends JUnitSuite {

  @Test def SubjectIsAChannel(): Unit = {

    var lastA: Integer = null
    var errorA: Throwable = null
    var completedA: Boolean = false
    val observerA = Observer[Integer](
      (next: Integer) => { lastA = next },
      (error: Throwable) => { errorA = error },
      () => { completedA = true }
    )

    var lastB: Integer = null
    var errorB: Throwable = null
    var completedB: Boolean = false
    val observerB = Observer[Integer](
      (next: Integer) => { lastB = next },
      (error: Throwable) => { errorB = error },
      () => { completedB = true }
    )

    var lastC: Integer = null
    var errorC: Throwable = null
    var completedC: Boolean = false
    val observerC = Observer[Integer](
      (next: Integer) => { lastC = next },
      (error: Throwable) => { errorC = error },
      () => { completedC = true }
    )

    val channel: Subject[Integer] = Subject[Integer]()

    val a = channel(observerA)
    val b = channel(observerB)

      assertEquals(null, lastA)
      assertEquals(null, lastB)

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)

    a.unsubscribe()
    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onCompleted()

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    val c = channel.subscribe(observerC)
    channel.onNext(13)

      assertEquals(null, lastC)
      assertTrue(completedC)

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onError(new Exception("!"))

      assertEquals(null, lastC)
      assertTrue(completedC)

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)
  }

  @Test def ReplaySubjectIsAChannel(): Unit = {

    val channel = ReplaySubject[Integer]

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false

    val b = channel(new Observer[Integer] {
      override def onNext(value: Integer): Unit = { lastB = value }
      override def onError(error: Throwable): Unit = { errorB = true }
      override def onCompleted(): Unit = { completedB = true }
    })

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)
    
    a.unsubscribe()

    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)
    
    channel.onCompleted()

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onNext(13)

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)
  }

  @Test def BehaviorSubjectIsACache(): Unit = {

    val channel = BehaviorSubject(2013)

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

      assertEquals(2013, lastA)
      assertEquals(2013, lastB)

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)

    a.unsubscribe()

    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onCompleted()

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onNext(13)

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

  }

  @Test def AsyncSubjectIsAFuture(): Unit = {

    val channel = AsyncSubject[Int]()

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

    channel.onNext(42)

      Assert.assertEquals(null, lastA)
      Assert.assertEquals(null, lastB)

    a.unsubscribe()
    channel.onNext(4711)
    channel.onCompleted()

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)


    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

    channel.onNext(13)

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

  }

  @Test def UnicastSubjectIsABuffer(): Unit = {

    val channel = UnicastSubject[Integer]
    channel.onNext(42)

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    assertEquals(42, lastA)

    channel.onNext(4711)
    assertEquals(4711, lastA)

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

    assertEquals(null, lastB)
    assertFalse(completedB)
    assertTrue(errorB) // only a single subscriber is allowed

    val channel2 = UnicastSubject[Integer]
    channel2.onNext(13)
    channel2.onCompleted()

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel2.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

    assertEquals(13, lastC)
    assertTrue(completedC)
    assertFalse(errorC)

    val channel3 = UnicastSubject[Integer]
    channel3.onError(new Exception("Boom"))

    var lastD: Integer = null
    var errorD, completedD: Boolean = false
    val d = channel3.subscribe(x => { lastD = x}, e => { errorD = true} , () => { completedD = true })

    assertEquals(null, lastD)
    assertFalse(completedD)
    assertTrue(errorD)
  }
}
