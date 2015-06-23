/**
 * Copyright 2015 Netflix, Inc.
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
package rx.lang.scala.completeness

import scala.reflect.runtime.universe.typeOf

class TestSubscriberCompletenessKit extends CompletenessKit {
  override val rxJavaType = typeOf[rx.observers.TestSubscriber[_]]

  override val rxScalaType = typeOf[rx.lang.scala.observers.TestSubscriber[_]]

  override protected def correspondenceChanges: Map[String, String] = Map(
    "assertCompleted()" -> "assertCompleted()",
    "assertError(Class[_ <: Throwable])" -> "assertError(Class[_ <: Throwable])",
    "assertNoErrors()" -> "assertNoErrors()",
    "assertNoTerminalEvent()" -> "assertNoTerminalEvent()",
    "assertNoValues()" -> "assertNoValues()",
    "assertNotCompleted()" -> "assertNotCompleted()",
    "assertReceivedOnNext(List[T])" -> "assertValues(T*)",
    "assertTerminalEvent()" -> "assertTerminalEvent()",
    "assertUnsubscribed()" -> "assertUnsubscribed()",
    "awaitTerminalEvent()" -> "awaitTerminalEvent()",
    "getOnCompletedEvents()" -> "assertCompleted()",
    "onCompleted()" -> "onCompleted()",
    "onStart()" -> "onStart()",
    "unsubscribe()" -> "unsubscribe()"
  )
}
