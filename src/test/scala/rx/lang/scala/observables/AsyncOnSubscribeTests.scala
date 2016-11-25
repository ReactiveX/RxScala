package rx.lang.scala.observables

import org.junit.Test
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.{Notification, Observable}

class AsyncOnSubscribeTests extends JUnitSuite {

  @Test
  def testApply(): Unit = {
    val last = 2000L
    val sut = AsyncOnSubscribe(() => 0L)((count,demand) =>
      if(count > last)
        (Notification.OnCompleted, count)
      else {
        val max = math.max(count + demand, last)
        val next = Observable.from(count to max)
        (Notification.OnNext(next), max+1)
      }
    )
    assertEquals((0L to last).toList, sut.toBlocking.toList)
  }

  @Test
  def testCreateStateful(): Unit = {
    val last = 2000L
    val sut = AsyncOnSubscribe.createStateful[Long, Long](() => 0L)((count,demand, obs) =>
      if(count > last) {
        obs.onCompleted()
        count
      } else {
        val max = math.max(count + demand, last)
        val next = Observable.from(count to max)
        obs.onNext(next)
        max+1
      }
    )
    assertEquals((0L to last).toList, sut.toBlocking.toList)
  }

  @Test
  def testApplyStateless(): Unit = {
    val sut = AsyncOnSubscribe.applyStateless(r => Notification.OnNext(Observable.just(42).repeat(r)))
    assertEquals(List(42,42,42,42), sut.take(4).toBlocking.toList)
  }

  @Test
  def testCreateStateless(): Unit = {
    val sut = AsyncOnSubscribe.createStateless[Int]((r, obs) => obs.onNext(Observable.just(42).repeat(r)))
    assertEquals(List(42,42,42,42), sut.take(4).toBlocking.toList)
  }

  @Test
  def testApplySingleState(): Unit = {
    val random = math.random
    val sut = AsyncOnSubscribe.applySingleState(() => random)((s,r) => Notification.OnNext(Observable.just(random.toString).repeat(r)))
    assertEquals(List(random.toString, random.toString), sut.take(2).toBlocking.toList)
  }

  @Test
  def testUnsubscribe(): Unit = {
    val sideEffect = new java.util.concurrent.atomic.AtomicBoolean(false)
    val sut = AsyncOnSubscribe(() => ())((s,r) => (Notification.OnCompleted, s), onUnsubscribe = s => sideEffect.set(true))
    sut.foreach(_ => ())
    assertEquals(true, sideEffect.get())
  }
}
