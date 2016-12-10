package rx.lang.scala.observables

import org.junit.Test
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Notification, Observable}

class AsyncOnSubscribeTests extends JUnitSuite {

  @Test
  def testStateful(): Unit = {
    val last = 2000L
    val o = Observable.create(AsyncOnSubscribe(() => 0L)((count,demand) =>
      if(count > last)
        (Notification.OnCompleted, count)
      else {
        val to = math.min(count+demand, last+1)
        val next = Observable.from(count until to)
        (Notification.OnNext(next), to)
      }
    ))
    assertEquals((0L to last).toList, o.toBlocking.toList)
  }

  @Test
  def testStateless(): Unit = {
    val o = Observable.create(AsyncOnSubscribe.stateless(r => Notification.OnNext(Observable.just(42).repeat(r))))
    assertEquals(List(42,42,42,42), o.take(4).toBlocking.toList)
  }

  @Test
  def testSingleState(): Unit = {
    val random = math.random
    val o = Observable.create(AsyncOnSubscribe.singleState(() => random)((s,r) => Notification.OnNext(Observable.just(random.toString).repeat(r))))
    assertEquals(List(random.toString, random.toString), o.take(2).toBlocking.toList)
  }

  @Test
  def testUnsubscribe(): Unit = {
    val sideEffect = new java.util.concurrent.atomic.AtomicBoolean(false)
    val o = Observable.create(AsyncOnSubscribe(() => ())((s,r) => (Notification.OnCompleted, s), onUnsubscribe = s => sideEffect.set(true)))
    o.foreach(_ => ())
    assertEquals(true, sideEffect.get())
  }

  @Test
  def testError(): Unit = {
    val e = new IllegalStateException("Oh noes")
    val o = Observable.create(AsyncOnSubscribe(() => 0)((s,_) => (if(s>2) Notification.OnNext(Observable.just(s)) else Notification.OnError(e), s+1)))
    val testSubscriber = TestSubscriber[Int]()
    o.subscribe(testSubscriber)
    testSubscriber.assertError(e)
  }

  @Test
  // Ensure that the generator is executed for each subscription
  def testGenerator(): Unit = {
    val sideEffectCount = new java.util.concurrent.atomic.AtomicInteger(0)
    val o = Observable.create(AsyncOnSubscribe(() => sideEffectCount.incrementAndGet())((s, _) => (Notification.OnCompleted, s)))
    o.toBlocking.toList
    o.toBlocking.toList
    assertEquals(sideEffectCount.get(), 2)
  }
}
