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
package rx.lang.scala.schedulers

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Executors, ThreadFactory, TimeUnit}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

class ExecutionContextSchedulerTest extends JUnitSuite {

  private val prefix = "ExecutionContextSchedulerTest"
  private val threadFactory = new ThreadFactory {

    private val id = new AtomicLong(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, prefix + id.incrementAndGet())
      t.setDaemon(true)
      t
    }
  }
  private val scheduler = ExecutionContextScheduler(ExecutionContext.fromExecutor(Executors.newCachedThreadPool(threadFactory)))

  @Test
  def testSchedule(): Unit = {
    val worker = scheduler.createWorker
    val latch = new CountDownLatch(1)
    @volatile var threadName: String = null
    worker.schedule {
      threadName = Thread.currentThread.getName
      latch.countDown()
    }
    latch.await(30, TimeUnit.SECONDS)
    assert(threadName.startsWith(prefix))
  }

  @Test
  def testScheduleWithDelay(): Unit = {
    val worker = scheduler.createWorker
    val latch = new CountDownLatch(1)
    @volatile var threadName: String = null
    worker.schedule(1 millisecond) {
      threadName = Thread.currentThread.getName
      latch.countDown()
    }
    latch.await(30, TimeUnit.SECONDS)
    assert(threadName.startsWith(prefix))
  }

  @Test
  def testSchedulePeriodically(): Unit = {
    val worker = scheduler.createWorker
    val latch = new CountDownLatch(1)
    @volatile var threadName: String = null
    val subscription = worker.schedulePeriodically(1 millisecond, 1 millisecond) {
      threadName = Thread.currentThread.getName
      latch.countDown()
    }
    latch.await(30, TimeUnit.SECONDS)
    assert(threadName.startsWith(prefix))
    subscription.unsubscribe()
  }
}
