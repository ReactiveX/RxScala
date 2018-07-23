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

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader
import org.junit.Assert.assertEquals
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.junit.JUnitSuite

import scala.util.Try
class SingleTests extends JUnitSuite {


  @Test def testJust(): Unit = {
    val o = Single just 7
    assertEquals(7, o.toBlocking.single)
  }

  @Test def testFromFuture(): Unit = {
    val o = Single from Future { 5 }
    assertEquals(5, o.toBlocking.single)
  }

  @Test def testFromFutureWithDelay(): Unit = {
    val o = Single from Future { Thread.sleep(200); 42 }
    assertEquals(42, o.toBlocking.single)
  }


  @Test def testFromTry(): Unit = {
    val o = Single.from(Try { 13 })
    assertEquals(13, o.toBlocking.single)
  }

  @Test def testAppendHead(): Unit = {

    val o = 1+:Single.just(2)
    assertEquals(1, o.toBlocking.first)
    assertEquals(2, o.toBlocking.last)

  }

  @Test def testAppendLast(): Unit = {

    val o = Single.just(1):+2
    assertEquals(1, o.toBlocking.first)
    assertEquals(2, o.toBlocking.last)

  }

  @Test def testConcatSingles(): Unit = {

    val o = Single.just(1)++Single.just(2)
    assertEquals(1, o.toBlocking.first)
    assertEquals(2, o.toBlocking.last)

  }
}
