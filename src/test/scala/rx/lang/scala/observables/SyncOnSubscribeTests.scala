package rx.lang.scala.observables

import org.junit.Test
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.{Notification, Observer}

class SyncOnSubscribeTests extends JUnitSuite {

  @Test
  def testApply(): Unit = {
    val sut = SyncOnSubscribe(() => 0)(count =>
      if(count > 3)
        (Notification.OnCompleted, count)
      else
        (Notification.OnNext(count), count+1)
    )
    assertEquals(List(0,1,2,3), sut.toBlocking.toList)
  }

  @Test
  def testCreateStateful(): Unit = {
    val sut = SyncOnSubscribe.createStateful(() => 0)((count, obs: Observer[_ >: Int]) => {
      if(count > 3) {
        obs.onCompleted()
        count
      } else {
        obs.onNext(count)
        count+1
      }
    })
    assertEquals(List(0,1,2,3), sut.toBlocking.toList)
  }

  @Test
  def testApplyStateless(): Unit = {
    val sut = SyncOnSubscribe.applyStateless(() => Notification.OnNext(42))
    assertEquals(List(42,42,42,42), sut.take(4).toBlocking.toList)
  }

  @Test
  def testCreateStateless(): Unit = {
    val sut = SyncOnSubscribe.createStateless[Int](obs => obs.onNext(42))
    assertEquals(List(42,42,42,42), sut.take(4).toBlocking.toList)
  }

  @Test
  def testApplySingleState(): Unit = {
    val random = math.random
    val sut = SyncOnSubscribe.applySingleState(() => random)(s => Notification.OnNext(s.toString))
    assertEquals(List(random.toString, random.toString), sut.take(2).toBlocking.toList)
  }

  @Test
  def testUnsubscribe(): Unit = {
    val sideEffect = new java.util.concurrent.atomic.AtomicBoolean(false)
    val sut = SyncOnSubscribe(() => ())(s => (Notification.OnCompleted, s), onUnsubscribe = s => sideEffect.set(true))
    sut.foreach(_ => ())
    assertEquals(true, sideEffect.get())
  }
}
