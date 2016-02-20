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

package rx.lang.scala.observables

import rx.annotations.Beta
import rx.lang.scala.{Observable, Subscription, Subscriber}
import rx.lang.scala.JavaConversions._

/**
 * A [[ConnectableObservable]] resembles an ordinary [[Observable]], except that it does not begin
 * emitting items when it is subscribed to, but only when its [[ConnectableObservable.connect connect]] method is called.
 * In this way you can wait for all intended [[Subscriber]]s to subscribe to the [[Observable]]
 * before the [[Observable]] begins emitting items.
 *
 * <img width="640" height="510" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/publishConnect.png" alt="">
 *
 * @define beta
 * <span class="badge badge-red" style="float: right;">BETA</span>
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators">RxJava Wiki: Connectable Observable Operators</a>
 * @tparam T the type of items emitted
 */
class ConnectableObservable[+T] private[scala](val asJavaObservable: rx.observables.ConnectableObservable[_ <: T])
  extends Observable[T] {

  /**
   * Call a ConnectableObservable's connect method to instruct it to begin emitting the
   * items from its underlying [[rx.lang.scala.Observable]] to its [[rx.lang.scala.Observer]]s.
   */
  def connect: Subscription = toScalaSubscription(asJavaObservable.connect())

  /**
   * Returns an observable sequence that stays connected to the source as long
   * as there is at least one subscription to the observable sequence.
   *
   * @return a [[rx.lang.scala.Observable]]
   */
  def refCount: Observable[T] = toScalaObservable[T](asJavaObservable.refCount())

  /**
   * $beta Return an [[Observable]] that automatically connects to this [[ConnectableObservable]]
   * when the first [[Subscriber]] subscribes.
   *
   * @return an [[Observable]] that automatically connects to this [[ConnectableObservable]]
   *         when the first [[Subscriber]] subscribes
   */
  @Beta
  def autoConnect: Observable[T] = {
    toScalaObservable(asJavaObservable.autoConnect())
  }

  /**
   * $beta Return an [[Observable]] that automatically connects to this [[ConnectableObservable]]
   * when the specified number of [[Subscriber]]s subscribe to it.
   *
   * @param numberOfSubscribers the number of [[Subscriber]]s to await before calling connect
   *                            on the [[ConnectableObservable]]. A non-positive value indicates
   *                            an immediate connection.
   * @return an [[Observable]] that automatically connects to this [[ConnectableObservable]]
   *         when the specified number of [[Subscriber]]s subscribe to it
   */
  @Beta
  def autoConnect(numberOfSubscribers: Int): Observable[T] = {
    toScalaObservable(asJavaObservable.autoConnect(numberOfSubscribers))
  }
}
