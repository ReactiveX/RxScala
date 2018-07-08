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

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import org.junit.Assert._
import org.junit.{ Ignore, Test }
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import rx.lang.scala.schedulers.TestScheduler

class ObservableTests extends JUnitSuite {

  // Tests which needn't be run:

  @Ignore
  def testCovariance = {
    //println("hey, you shouldn't run this test")

    val o1: Observable[Nothing] = Observable.empty
    val o2: Observable[Int] = o1
    val o3: Observable[App] = o1
    val o4: Observable[Any] = o2
    val o5: Observable[Any] = o3
  }

  // Tests which have to be run:

  @Test
  def testDematerialize(): Unit = {
    val o = List(1, 2, 3).toObservable
    val mat = o.materialize
    val demat = mat.dematerialize

    //correctly rejected:
    //val wrongDemat = Observable("hello").dematerialize

    assertEquals(demat.toBlocking.toIterable.toList, List(1, 2, 3))
}

  @Test def TestScan(): Unit = {
     val xs = Observable.just(0,1,2,3)
     val ys = xs.scan(0)(_+_)
     assertEquals(List(0,0,1,3,6), ys.toBlocking.toList)
     val zs = xs.scan((x: Int, y:Int) => x*y)
     assertEquals(List(0, 0, 0, 0), zs.toBlocking.toList)
  }

  // Test that Java's firstOrDefault propagates errors.
  // If this changes (i.e. it suppresses errors and returns default) then Scala's firstOrElse
  // should be changed accordingly.
  @Test def testJavaFirstOrDefault(): Unit = {
    assertEquals(1, rx.Observable.just(1, 2).firstOrDefault(10).toBlocking().single)
    assertEquals(10, rx.Observable.empty().firstOrDefault(10).toBlocking().single)
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      rx.Observable.error(new Exception(msg)).firstOrDefault(10).toBlocking().single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFirstOrElse(): Unit = {
    def mustNotBeCalled: String = sys.error("this method should not be called")
    def mustBeCalled: String = "this is the default value"
    assertEquals("hello", Observable.just("hello").firstOrElse(mustNotBeCalled).toBlocking.single)
    assertEquals("this is the default value", Observable.empty.firstOrElse(mustBeCalled).toBlocking.single)
  }

  @Test def testTestWithError(): Unit = {
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      Observable.error(new Exception(msg)).firstOrElse(10).toBlocking.single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFromFuture(): Unit = {
    val o = Observable from Future { 5 }
    assertEquals(5, o.toBlocking.single)
  }

  @Test def testFromFutureWithDelay(): Unit = {
    val o = Observable from Future { Thread.sleep(200); 42 }
    assertEquals(42, o.toBlocking.single)
  }

  @Test def testFromFutureWithError(): Unit = {
    val err = new Exception("ooops42")
    val o: Observable[Int] = Observable from Future { Thread.sleep(200); throw err }
    assertEquals(List(Notification.OnError(err)), o.materialize.toBlocking.toList)
  }

  @Test def testFromFutureWithSubscribeOnlyAfterCompletion(): Unit = {
    val f = Future { Thread.sleep(200); 6 }
    val o = Observable from f
    val res = Await.result(f, Duration.Inf)
    assertEquals(6, res)
    assertEquals(6, o.toBlocking.single)
  }

  @Test def testJoin(): Unit = {
     val xs = Observable.just(1,2,3)
     val ys = Observable.just("a")
     val zs = xs.join(ys)(_ => Observable.never, _ => Observable.never, (x, y) => y + x)
     assertEquals(List("a1", "a2", "a3"),zs.toBlocking.toList)
  }

  @Test def testTimestampWithScheduler(): Unit = {
    val c = 10
    val s = TestScheduler()
    val o1 = Observable interval (1.milliseconds, s) map (_ + 1)
    val o2 = o1 timestamp s
    val l = ListBuffer[(Long, Long)]()
    o2.subscribe (
      onNext = (l += _)
    )
    s advanceTimeTo c.milliseconds
    val (l1, l2) = l.toList.unzip
    assertTrue(l1.size == c)
    assertEquals(l2, l1)
  }

  @Test def testHead(): Unit = {
    val o: Observable[String] = List("alice", "bob", "carol").toObservable.head
    assertEquals(List("alice"), o.toBlocking.toList)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testHeadWithEmptyObservable(): Unit = {
    val o: Observable[String] = List[String]().toObservable.head
    o.toBlocking.toList
  }

  @Test def testTail(): Unit = {
    val o: Observable[String] = List("alice", "bob", "carol").toObservable.tail
    assertEquals(List("bob", "carol"), o.toBlocking.toList)
    assertEquals(List("bob", "carol"), o.toBlocking.toList)
  }

  @Test(expected = classOf[UnsupportedOperationException])
  def testTailWithEmptyObservable(): Unit = {
    val o: Observable[String] = List[String]().toObservable.tail
    o.toBlocking.toList
  }

  @Test
  def testZipWithIndex(): Unit = {
    val o = List("alice", "bob", "carol").toObservable.zipWithIndex.map(_._2)
    assertEquals(List(0, 1, 2), o.toBlocking.toList)
    assertEquals(List(0, 1, 2), o.toBlocking.toList)
  }

  @Test
  def testSingleOrElse(): Unit = {
    val o = Observable.just(1).singleOrElse(2)
    assertEquals(1, o.toBlocking.single)
  }

  @Test
  def testSingleOrElseWithEmptyObservable(): Unit = {
    val o: Observable[Int] = Observable.empty.singleOrElse(1)
    assertEquals(1, o.toBlocking.single)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOrElseWithTooManyItems(): Unit = {
    Observable.just(1, 2).singleOrElse(1).toBlocking.single
  }

  @Test
  def testSingleOrElseWithCallByName(): Unit = {
    var called = false
    val o: Observable[Int] = Observable.empty.singleOrElse {
      called = true
      1
    }
    assertFalse(called)
    o.subscribe()
    assertTrue(called)
  }

  @Test
  def testSingleOrElseWithCallByName2(): Unit = {
    var called = false
    val o = Observable.just(1).singleOrElse {
      called = true
      2
    }
    assertFalse(called)
    o.subscribe()
    assertFalse(called)
  }

  @Test
  def testOrElse(): Unit = {
    val o = Observable.just(1, 2, 3).orElse(4)
    assertEquals(List(1, 2, 3), o.toBlocking.toList)
  }

  @Test
  def testOrElseWithEmpty(): Unit = {
    val o = Observable.empty.orElse(-1)
    assertEquals(List(-1), o.toBlocking.toList)
  }

  @Test
  def testToMultiMap(): Unit = {
    val o = Observable.just("a", "b", "cc", "dd").toMultiMap(_.length)
    val expected = Map(1 -> Set("a", "b"), 2 -> Set("cc", "dd"))
    assertEquals(expected, o.toBlocking.single)
  }

  @Test
  def testToMultiMapWithValueSelector(): Unit = {
    val o = Observable.just("a", "b", "cc", "dd").toMultiMap(_.length, s => s + s)
    val expected = Map(1 -> Set("aa", "bb"), 2 -> Set("cccc", "dddd"))
    assertEquals(expected, o.toBlocking.single)
  }

  @Test
  def testToMultiMapWithMapFactory(): Unit = {
    val m = new mutable.LinkedHashMap[Int, mutable.Set[String]] with mutable.MultiMap[Int, String]
    val o = Observable.just("a", "b", "cc", "dd").toMultiMap(_.length, s => s, m)
    val expected = Map(1 -> Set("a", "b"), 2 -> Set("cc", "dd"))
    val r = o.toBlocking.single
    // r should be the same instance created by the `multiMapFactory`
    assertTrue(m eq r)
    assertEquals(expected, r)
  }

  @Test
  def testToTraversable(): Unit = {
    val o = Observable.just(1, 2, 3).toTraversable
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToList(): Unit = {
    val o = Observable.just(1, 2, 3).toList
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIterable(): Unit = {
    val o = Observable.just(1, 2, 3).toIterable
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIterator(): Unit = {
    val o = Observable.just(1, 2, 3).toIterator
    assertEquals(Seq(1, 2, 3), o.toBlocking.single.toSeq)
  }

  @Test
  def testToStream(): Unit = {
    val o = Observable.just(1, 2, 3).toStream
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIndexedSeq(): Unit = {
    val o = Observable.just(1, 2, 3).toIndexedSeq
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToBuffer(): Unit = {
    val o = Observable.just(1, 2, 3).toBuffer
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToSet(): Unit = {
    val o = Observable.just(1, 2, 2).toSet
    assertEquals(Set(1, 2), o.toBlocking.single)
  }

  @Test
  def testToVector(): Unit = {
    val o = Observable.just(1, 2, 3).toVector
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToArray(): Unit = {
    val o = Observable.just(1, 2, 3).toArray
    assertArrayEquals(Array(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testFilterNot(): Unit = {
    val o = Observable.just(1, 2, 3).filterNot(_ > 2)
    assertEquals(List(1, 2), o.toBlocking.toList)
  }

  @Test
  def testCount(): Unit = {
    assertEquals(1, Observable.just(1, 2, 3).count(_ > 2).toBlocking.single)
    assertEquals(2, Observable.just(1, 2, 3).count(_ <= 2).toBlocking.single)
  }

  @Test
  def testNonEmpty(): Unit = {
    assertEquals(false, Observable.empty.nonEmpty.toBlocking.single)
    assertEquals(true, Observable.just(1, 2, 3).nonEmpty.toBlocking.single)
  }

  @Test
  def testTailWithBackpressure(): Unit = {
    val result = mutable.ListBuffer[Int]()
    var completed = false
    var error = false
    Observable.just(1, 2).tail.subscribe(new Subscriber[Int] {
      override def onStart(): Unit = request(1)
      override def onNext(v: Int): Unit = {
        result += v
        request(1)
      }
      override def onError(e: Throwable): Unit = error = true
      override def onCompleted(): Unit = completed = true
    })
    assertEquals(List(2), result)
    assertTrue(completed)
    assertFalse(error)
  }

  @Test
  def testToListWithBackpressure(): Unit = {
    var result: List[Int] = null
    var completed = false
    var error = false
    Observable.just(1, 2).toList.subscribe(new Subscriber[List[Int]] {
      override def onStart(): Unit = request(1)

      override def onNext(v: List[Int]): Unit = {
        result = v
        request(1)
      }

      override def onError(e: Throwable): Unit = error = true

      override def onCompleted(): Unit = completed = true
    })
    assertEquals(List(1, 2), result)
    assertTrue(completed)
    assertFalse(error)
  }

  @Test
  def testToMultimapWithBackpressure(): Unit = {
    var result: mutable.MultiMap[Int, Int] = null
    var completed = false
    var error = false
    Observable.just(1, 2, 3, 4).toMultiMap(_ % 2).subscribe(new Subscriber[mutable.MultiMap[Int, Int]] {
      override def onStart(): Unit = request(1)

      override def onNext(v: mutable.MultiMap[Int, Int]): Unit = {
        result = v
        request(1)
      }

      override def onError(e: Throwable): Unit = error = true

      override def onCompleted(): Unit = completed = true
    })
    val expected = Map(0 -> Set(2, 4), 1 -> Set(1, 3))
    assertEquals(expected, result)
    assertTrue(completed)
    assertFalse(error)
  }

  @Test
  def testToMap(): Unit = {
    val expectedMap1 = (0 to 100).map(i => (i % 2, i)).toMap
    val m1 = (0 to 100).toObservable.toMap(_ % 2)
    assertEquals(expectedMap1, m1.toBlocking.single)

    val expectedMap2 = (0 to 100).map(i => (i % 2, i * 100)).toMap
    val m2 = (0 to 100).toObservable.toMap(_ % 2, _ * 100)
    assertEquals(expectedMap2, m2.toBlocking.single)

    val expectedMap3 = (0 to 100).map(i => (i % 2, i * 100)).toMap
    val m3 = (0 to 100).toObservable.map(i => (i % 2, i * 100)).toMap
    assertEquals(expectedMap3, m3.toBlocking.single)
  }

}
