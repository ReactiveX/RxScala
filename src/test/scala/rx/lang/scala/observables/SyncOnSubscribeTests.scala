package rx.lang.scala.observables

import org.junit.Test
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.{Notification, Observable}

class SyncOnSubscribeTests extends JUnitSuite {

  @Test
  def testStateful(): Unit = {
    val o = Observable.create(SyncOnSubscribe(() => 0)(count =>
      if(count > 3)
        (Notification.OnCompleted, count)
      else
        (Notification.OnNext(count), count+1)
    ))
    assertEquals(List(0,1,2,3), o.toBlocking.toList)
  }

  @Test
  def testStateless(): Unit = {
    val o = Observable.create(SyncOnSubscribe.stateless(() => Notification.OnNext(42)))
    assertEquals(List(42,42,42,42), o.take(4).toBlocking.toList)
  }

  @Test
  def testSingleState(): Unit = {
    val random = math.random
    val o = Observable.create(SyncOnSubscribe.singleState(() => random)(s => Notification.OnNext(s.toString)))
    assertEquals(List(random.toString, random.toString), o.take(2).toBlocking.toList)
  }

  @Test
  def testUnsubscribe(): Unit = {
    val sideEffect = new java.util.concurrent.atomic.AtomicBoolean(false)
    val o = Observable.create(SyncOnSubscribe(() => ())(s => (Notification.OnCompleted, s), onUnsubscribe = s => sideEffect.set(true)))
    o.foreach(_ => ())
    assertEquals(true, sideEffect.get())
  }

  @Test
  def testError(): Unit = {
    val e = new IllegalStateException("Oh noes")
    val o = Observable.create(SyncOnSubscribe(() => 0)(s => (if(s>2) Notification.OnNext(s) else Notification.OnError(e), s+1)))
    val testSubscriber = TestSubscriber[Int]()
    o.subscribe(testSubscriber)
    testSubscriber.assertError(e)
  }

  @Test
  // Ensure that the generator is executed for each subscription
  def testGenerator(): Unit = {
    val sideEffectCount = new java.util.concurrent.atomic.AtomicInteger(0)
    val o = Observable.create(SyncOnSubscribe(() => sideEffectCount.incrementAndGet())(s => (Notification.OnCompleted, s)))
    o.toBlocking.toList
    o.toBlocking.toList
    assertEquals(sideEffectCount.get(), 2)
  }
}
