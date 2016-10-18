/**
  * Copyright 2013 Netflix, Inc.
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
package rx.lang.scala.subjects

import rx.annotations.Experimental
import rx.lang.scala.Subject

/**
  * $experimental A `Subject` variant which buffers events until a single `Subscriber` arrives and replays
  * them to it and potentially switches to direct delivery once the `Subscriber` caught up and requested an
  * unlimited amount. In this case, the buffered values are no longer retained. If the `Subscriber` requests
  * a limited amount, queueing is involved and only those values are retained which weren't requested by the
  * `Subscriber` at that time.
  *
  * @define experimental
  * <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>
  */
@Experimental
object UnicastSubject {

  /**
    * $experimental Constructs an empty `UnicastSubject` instance with the default capacity hint of 16 elements.
    *
    * @tparam T the input and output value type
    * @return the created `UnicastSubject` instance
    */
  @Experimental
  def apply[T](): UnicastSubject[T] = new UnicastSubject[T](rx.subjects.UnicastSubject.create[T]())

  /**
    * $experimental Constructs an empty UnicastSubject instance with a capacity hint.
    * <p>The capacity hint determines the internal queue's island size: the larger
    * it is the less frequent allocation will happen if there is no subscriber
    * or the subscriber hasn't caught up.
    *
    * @param capacity the capacity hint for the internal queue
    * @tparam T the input and output value type
    * @return the created `UnicastSubject` instance
    */
  @Experimental
  def apply[T](capacity: Int): UnicastSubject[T] = new UnicastSubject[T](rx.subjects.UnicastSubject.create(capacity))
}

private [scala] class UnicastSubject[T] private [scala] (val asJavaSubject: rx.subjects.UnicastSubject[T]) extends Subject[T] {}
