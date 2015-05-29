/**
 * Copyright 2014 Netflix, Inc.
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
package rx.lang.scala.examples

import scala.concurrent.duration._
import scala.language.postfixOps

import org.junit.Ignore
import org.junit.Test

import rx.lang.scala._
import rx.lang.scala.schedulers._

@Ignore
object ExperimentalAPIExamples {

  @Test def noBackpressureExample(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).observeOn(IOScheduler()).subscribe(
      v => {
        Thread.sleep(10) // A slow consumer
        println(s"process $v")
      },
      e => e.printStackTrace()
    )
  }

  @Test def onBackpressureBlockExample(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).doOnNext(v => println(s"emit $v")).onBackpressureBlock.observeOn(IOScheduler()).subscribe {
      v =>
        Thread.sleep(10) // A slow consumer
        println(s"process $v")
    }
  }

  @Test def onBackpressureBlockExample2(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).doOnNext(v => println(s"emit $v")).onBackpressureBlock(10).observeOn(IOScheduler()).subscribe {
      v =>
        Thread.sleep(10) // A slow consumer
        println(s"process $v")
    }
  }

  @Test def doOnRequestExample(): Unit = {
    (1 to 300).toObservable.doOnRequest(request => println(s"request $request")).subscribe(new Subscriber[Int]() {
      override def onStart(): Unit = request(1)

      override def onNext(v: Int): Unit = request(1)

      override def onError(e: Throwable): Unit = e.printStackTrace()

      override def onCompleted(): Unit = {}
    })
  }

  @Test def onBackpressureBufferWithCapacityExample(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).onBackpressureBuffer(200).observeOn(IOScheduler()).subscribe {
      v =>
        Thread.sleep(10) // A slow consumer
        println(s"process $v")
    }
  }

  @Test def onBackpressureBufferWithCapacityExample2(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).onBackpressureBuffer(10, println("Overflow")).observeOn(IOScheduler()).subscribe(
      v => {
        Thread.sleep(10)
        // A slow consumer
        println(s"process $v")
      },
      e => e.printStackTrace()
    )
  }

  @Test def switchIfEmptyExample(): Unit = {
    val o1: Observable[Int] = Observable.empty
    val o2 = Observable.just(1, 3, 5)
    val alternate = Observable.just(2, 4, 6)
    o1.switchIfEmpty(alternate).foreach(println)
    o2.switchIfEmpty(alternate).foreach(println)
  }

  @Test def withLatestFromExample(): Unit = {
    val a = Observable.interval(1 second).take(7)
    val b = Observable.interval(250 millis)
    a.withLatestFrom(b)((x, y) => (x, y)).toBlocking.foreach {
      case (x, y) => println(s"a: $x b: $y")
    }
  }

  @Test def withLatestFromExample2(): Unit = {
    val a = Observable.interval(1 second).take(7)
    val b = Observable.timer(3 seconds, 250 millis)
    a.withLatestFrom(b)((x, y) => (x, y)).toBlocking.foreach {
      case (x, y) => println(s"a: $x b: $y")
    }
  }

  @Test def takeUntilExample(): Unit = {
    Observable.just(1, 2, 3, 4, 5, 6, 7).
      doOnNext(v => println(s"onNext: $v")).
      takeUntil((x: Int) => x == 3). // why the compiler cannot infer `int`?
      foreach(println)
  }

  @Test def flatMapWithMaxConcurrentExample(): Unit = {
    (1 to 1000000).toObservable
      .doOnNext(v => println(s"Emitted Value: $v"))
      .flatMap(maxConcurrent = 10, (v: Int) => Observable.just(v).doOnNext(_ => Thread.sleep(1)).subscribeOn(IOScheduler()))
      .toBlocking.foreach(v => System.out.println("Received: " + v))
  }

  @Test def flatMapWithMaxConcurrentExample2(): Unit = {
    (1 to 1000000).toObservable
      .doOnNext(v => println(s"Emitted Value: $v"))
      .flatMap(
        maxConcurrent = 10,
        (v: Int) => Observable.just(v).doOnNext(_ => Thread.sleep(1)).subscribeOn(IOScheduler()),
        e => Observable.just(-1).doOnNext(_ => Thread.sleep(1)).subscribeOn(IOScheduler()),
        () => Observable.just(Int.MaxValue).doOnNext(_ => Thread.sleep(1)).subscribeOn(IOScheduler())
      )
      .toBlocking.foreach(v => System.out.println("Received: " + v))
  }

  @Test def flatMapWithMaxConcurrentExample3() {
    (1 to 1000000).toObservable
      .doOnNext(v => println(s"Emitted Value: $v"))
      .flatMapWith(
        maxConcurrent = 10,
        (v: Int) => Observable.just(v).doOnNext(_ => Thread.sleep(1)).subscribeOn(IOScheduler())
      )(_ * _).subscribeOn(IOScheduler())
      .toBlocking.foreach(v => System.out.println("Received: " + v))
  }

  @Test def onBackpressureDropDoExample(): Unit = {
    Observable[Int](subscriber => {
      (1 to 200).foreach(subscriber.onNext)
    }).onBackpressureDrop {
      t => println(s"Dropping $t")
    }.observeOn(IOScheduler()).subscribe {
      v =>
        Thread.sleep(10) // A slow consumer
        println(s"process $v")
    }
  }
}
