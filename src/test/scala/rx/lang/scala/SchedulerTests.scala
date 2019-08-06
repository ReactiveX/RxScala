package rx.lang.scala

import java.util.concurrent.TimeUnit

import org.junit.Assert.assertTrue
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite
import rx.lang.scala.schedulers.TestScheduler

class SchedulerTests extends JUnitSuite {

  @Test def testScheduleRecSingleRound(): Unit = {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    worker.scheduleRec({ count += 1; worker.unsubscribe() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 1)
  }

  @Test def testScheduleRecMultipleRounds(): Unit = {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    worker.scheduleRec({ count += 1; if(count == 100) worker.unsubscribe() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 100)
  }

  @Test def testScheduleRecUnsubscribe(): Unit = {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    val subscription = worker.scheduleRec({ count += 1 })
    subscription.unsubscribe()
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 0)
  }

  @Test(expected = classOf[Exception])
  def testScheduleRecException(): Unit = {
    val scheduler = TestScheduler()
    scheduler.createWorker.scheduleRec({ throw new Exception() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
  }

}
