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

  override val isOmittingParenthesesForArity0Method = false

  override protected def correspondenceChanges: Map[String, String] = Map(
    "assertError(Class[_ <: Throwable])" -> "assertError(Class[_ <: Throwable])",
    "assertReceivedOnNext(List[T])" -> "assertValues(T*)",
    "getLastSeenThread()" -> "getLastSeenThread",
    "getOnErrorEvents()" -> "getOnErrorEvents",
    "getOnNextEvents()" -> "getOnNextEvents",
    "isUnsubscribed()" -> "isUnsubscribed",
    "getCompletions()" -> "getCompletions",
    "getValueCount()" -> "[TODO]",
    "awaitValueCount(Int, Long, TimeUnit)" -> "[TODO]",

    "create()" -> "apply()",
    "create(Long)" -> "apply(Long)",
    "create(Observer[T])" -> "apply(Observer[T])",
    "create(Observer[T], Long)" -> "apply(Observer[T], Long)",
    "create(Subscriber[T])" -> "apply(Subscriber[T])"
  )
}
