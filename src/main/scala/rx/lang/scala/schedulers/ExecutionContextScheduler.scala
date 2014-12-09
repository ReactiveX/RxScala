/**
 * Copyright 2014 Netflix, Inc.
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
package rx.lang.scala.schedulers

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext
import rx.lang.scala.Scheduler

object ExecutionContextScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that executes work on the specified `ExecutionContext`.
   */
  def apply(executor: ExecutionContext): ExecutionContextScheduler =  {
    new ExecutionContextScheduler(rx.schedulers.Schedulers.from(new Executor {
      override def execute(command: Runnable): Unit = executor.execute(command)
    }))
  }
}

class ExecutionContextScheduler private[scala](val asJavaScheduler: rx.Scheduler) extends Scheduler
