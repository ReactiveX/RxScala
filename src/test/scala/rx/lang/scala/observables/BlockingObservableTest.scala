/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.scala.observables

import scala.concurrent.Await
import scala.concurrent.duration._
import org.junit.Assert._
import org.junit.{Test, Ignore}
import org.scalatest.junit.JUnitSuite
import scala.language.postfixOps
import rx.lang.scala.Observable

class BlockingObservableTest extends JUnitSuite {

  // Tests which needn't be run:

  @Ignore
  def testMostRecent(): Unit = {
    class Fruit {
      def printMe(): Unit = println(this)
    }
    class Apple extends Fruit
    class Pear extends Fruit
    
    val apples: Observable[Apple] = Observable.just(new Apple) ++ Observable.never
    
    // If we give a Pear as initial value for BlockingObservable[Apple].mostRecent,
    // the Iterable returned by mostRecent contains elements of the least common
    // supertype of Apple and Pear, which is Fruit.
    // This works because Scala allows upper bounds for type parameters.
    val firstFruit = apples.toBlocking.mostRecent(new Pear).head
    
    firstFruit.printMe()
  }

  // Tests which have to be run:

  @Test
  def testSingleOption(): Unit = {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.singleOption)
  }

  @Test
  def testSingleOptionWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.singleOption)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOptionWithMultipleItems(): Unit = {
    Observable.just(1, 2).toBlocking.singleOption
  }

  @Test
  def testSingleOrElse(): Unit = {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.singleOrElse(2))
  }

  @Test
  def testSingleOrElseWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.singleOrElse(2))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOrElseWithMultipleItems(): Unit = {
    Observable.just(1, 2).toBlocking.singleOrElse(2)
  }

  @Test
  def testHeadOption(): Unit = {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithMultipleItems(): Unit = {
    val o = Observable.just(1, 2)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOrElse(): Unit = {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithMultipleItems(): Unit = {
    val o = Observable.just(1, 2)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testLastOption(): Unit = {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithMultipleItems(): Unit = {
    val o = Observable.just(1, 2)
    assertEquals(Some(2), o.toBlocking.lastOption)
  }

  @Test
  def testLastOrElse(): Unit = {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithEmpty(): Unit = {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithMultipleItems(): Unit = {
    val o = Observable.just(1, 2)
    assertEquals(2, o.toBlocking.lastOrElse(3))
  }

  @Test
  def testToFuture(): Unit = {
    val o = Observable.just(1)
    val r = Await.result(o.toBlocking.toFuture, 10 seconds)
    assertEquals(1, r)
  }

  @Test
  def testToFutureWithEmpty(): Unit = {
    val o = Observable.empty
    val future = o.toBlocking.toFuture //if this were to throw the original test would wrongly succeed
    Await.result(future.failed, 10 seconds) match {
      case t:NoSuchElementException => //this is what we expect
      case _ => fail("expected the future to fail with a NoSuchElementException")
    }
  }

  @Test
  def testToFutureWithMultipleItems(): Unit = {
    val o = Observable.just(1, 2)
    val future = o.toBlocking.toFuture //if this were to throw the original test would wrongly succeed
    Await.result(future.failed, 10 seconds) match {
      case t:IllegalArgumentException => //this is what we expect
      case _ => fail("expected the future to fail with an IllegalArgumentException")
    }
  }
}
