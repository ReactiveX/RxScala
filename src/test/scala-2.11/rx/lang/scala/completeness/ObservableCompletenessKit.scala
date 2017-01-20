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

class ObservableCompletenessKit extends CompletenessKit {
  override val rxJavaType = typeOf[rx.Observable[_]]

  override val rxScalaType = typeOf[rx.lang.scala.Observable[_]]

  // some frequently used comments:
  val unnecessary = "[considered unnecessary in Scala land]"
  val deprecated = "[deprecated in RxJava]"
  val averageProblem = "[We can't have a general average method because Scala's `Numeric` does not have " +
    "scalar multiplication (we would need to calculate `(1.0/numberOfElements)*sum`). " +
    "You can use `fold` instead to accumulate `sum` and `numberOfElements` and divide at the end.]"
  val commentForFirstWithPredicate = "[use `.filter(condition).first`]"
  val fromFuture = "[TODO: Decide how Scala Futures should relate to Observables. Should there be a " +
    "common base interface for Future and Observable? And should Futures also have an unsubscribe method?]"
  val commentForTakeLastBuffer = "[use `takeRight(...).toSeq`]"
  val commentForToMultimapWithCollectionFactory = "[`toMultiMap` in RxScala returns `mutable.MultiMap`. It's a" +
    " `Map[A, mutable.Set[B]]`. You can override `def makeSet: Set[B]` to create a custom Set.]"
  val commentForRange = "[The `range` method of the Java Observable takes `start` and `count` parameters, " +
    "whereas the `range` method of the Scala Iterable takes `start` and `end` parameters, " +
    "so adding any of these two would be confusing. Moreover, since `scala.collection.immutable.Range` is " +
    "a subtype of `Iterable`, there are two nice ways of creating range Observables: " +
    "`(start to end).toObservable` or `Observable.from(start to end)`, and even more options are possible " +
    "using `until` and `by`.]"

  override def correspondenceChanges = Map(
    // manually added entries for Java instance methods
    "all(Func1[_ >: T, Boolean])" -> "forall(T => Boolean)",
    "ambWith(Observable[_ <: T])" -> "amb(Observable[U])",
    "asObservable()" -> unnecessary,
    "buffer(Int)" -> "tumblingBuffer(Int)",
    "buffer(Int, Int)" -> "slidingBuffer(Int, Int)",
    "buffer(Long, TimeUnit)" -> "tumblingBuffer(Duration)",
    "buffer(Long, TimeUnit, Int)" -> "tumblingBuffer(Duration, Int)",
    "buffer(Long, TimeUnit, Int, Scheduler)" -> "tumblingBuffer(Duration, Int, Scheduler)",
    "buffer(Long, TimeUnit, Scheduler)" -> "tumblingBuffer(Duration, Scheduler)",
    "buffer(Long, Long, TimeUnit)" -> "slidingBuffer(Duration, Duration)",
    "buffer(Long, Long, TimeUnit, Scheduler)" -> "slidingBuffer(Duration, Duration, Scheduler)",
    "buffer(Func0[_ <: Observable[_ <: TClosing]])" -> "tumblingBuffer(=> Observable[Any])",
    "buffer(Observable[B])" -> "tumblingBuffer(=> Observable[Any])",
    "buffer(Observable[B], Int)" -> "tumblingBuffer(Observable[Any], Int)",
    "buffer(Observable[_ <: TOpening], Func1[_ >: TOpening, _ <: Observable[_ <: TClosing]])" -> "slidingBuffer(Observable[Opening])(Opening => Observable[Any])",
    "cast(Class[R])" -> "[RxJava needs this one because `rx.Observable` is invariant. `Observable` in RxScala is covariant and does not need this operator.]",
    "collect(Func0[R], Action2[R, _ >: T])" -> "[TODO: See https://github.com/ReactiveX/RxScala/issues/63]",
    "compose(Transformer[_ >: T, _ <: R])" -> "[use extension methods instead]",
    "concatMapEager(Func1[_ >: T, _ <: Observable[_ <: R]], Int)" -> "concatMapEager(Int, T => Observable[R])",
    "concatMapEager(Func1[_ >: T, _ <: Observable[_ <: R]], Int, Int)" -> "concatMapEager(Int, Int, T => Observable[R])",
    "concatMapIterable(Func1[_ >: T, _ <: Iterable[_ <: R]])" -> "[use `concatMap(t => f(t).toObservable)`]",
    "concatMapDelayError(Func1[_ >: T, _ <: Observable[_ <: R]])" -> "[use `delayError.concatMap(T => Observable[R])`]",
    "concatWith(Observable[_ <: T])" -> "[use `o1 ++ o2`]",
    "contains(Any)" -> "contains(U)",
    "count()" -> "length",
    "debounce(Func1[_ >: T, _ <: Observable[U]])" -> "debounce(T => Observable[Any])",
    "defaultIfEmpty(T)" -> "orElse(=> U)",
    "delay(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]])" -> "delay(() => Observable[Any], T => Observable[Any])",
    "delay(Func1[_ >: T, _ <: Observable[U]])" -> "delay(T => Observable[Any])",
    "delaySubscription(Func0[_ <: Observable[U]])" -> "delaySubscription(() => Observable[Any])",
    "delaySubscription(Observable[U])" -> "[use `delaySubscription(() => Observable[Any])`]",
    "dematerialize()" -> "dematerialize(<:<[Observable[T], Observable[Notification[U]]])",
    "doOnCompleted(Action0)" -> "doOnCompleted(=> Unit)",
    "doOnEach(Action1[Notification[_ >: T]])" -> "[use `doOnEach(T => Unit, Throwable => Unit, () => Unit)`]",
    "doOnSubscribe(Action0)" -> "doOnSubscribe(=> Unit)",
    "doOnUnsubscribe(Action0)" -> "doOnUnsubscribe(=> Unit)",
    "doOnTerminate(Action0)" -> "doOnTerminate(=> Unit)",
    "elementAtOrDefault(Int, T)" -> "elementAtOrDefault(Int, U)",
    "to(Func1[_ >: Observable[T], R])" -> "[use Scala implicit feature to extend `Observable`]",
    "doAfterTerminate(Action0)" -> "doAfterTerminate(=> Unit)",
    "first(Func1[_ >: T, Boolean])" -> commentForFirstWithPredicate,
    "firstOrDefault(T)" -> "firstOrElse(=> U)",
    "firstOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `.filter(condition).firstOrElse(default)`]",
    "forEach(Action1[_ >: T])" -> "foreach(T => Unit)",
    "forEach(Action1[_ >: T], Action1[Throwable])" -> "foreach(T => Unit, Throwable => Unit)",
    "forEach(Action1[_ >: T], Action1[Throwable], Action0)" -> "foreach(T => Unit, Throwable => Unit, () => Unit)",
    "groupJoin(Observable[T2], Func1[_ >: T, _ <: Observable[D1]], Func1[_ >: T2, _ <: Observable[D2]], Func2[_ >: T, _ >: Observable[T2], _ <: R])" -> "groupJoin(Observable[S])(T => Observable[Any], S => Observable[Any], (T, Observable[S]) => R)",
    "ignoreElements()" -> "[use `filter(_ => false)`]",
    "join(Observable[TRight], Func1[T, Observable[TLeftDuration]], Func1[TRight, Observable[TRightDuration]], Func2[T, TRight, R])" -> "join(Observable[S])(T => Observable[Any], S => Observable[Any], (T, S) => R)",
    "last(Func1[_ >: T, Boolean])" -> "[use `filter(predicate).last`]",
    "lastOrDefault(T)" -> "lastOrElse(=> U)",
    "lastOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `filter(predicate).lastOrElse(default)`]",
    "lift(Operator[_ <: R, _ >: T])" -> "lift(Subscriber[R] => Subscriber[T])",
    "limit(Int)" -> "take(Int)",
    "flatMap(Func1[_ >: T, _ <: Observable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R])" -> "flatMapWith(T => Observable[U])((T, U) => R)",
    "flatMapIterable(Func1[_ >: T, _ <: Iterable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R])" -> "flatMapIterableWith(T => Iterable[U])((T, U) => R)",
    "flatMapIterable(Func1[_ >: T, _ <: Iterable[_ <: R]], Int)" -> "[use `flatMap(int, t => collectionSelector(t).toObservable)`]",
    "flatMapIterable(Func1[_ >: T, _ <: Iterable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R], Int)" -> "[use `flatMapWith(Int, t => collectionSelector(t).toObservable)(resultSelector)`]",
    "flatMap(Func1[_ >: T, _ <: Observable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R], Int)" -> "flatMapWith(Int, T => Observable[U])((T, U) => R)",
    "flatMap(Func1[_ >: T, _ <: Observable[_ <: R]], Int)" -> "flatMap(Int, T => Observable[R])",
    "flatMap(Func1[_ >: T, _ <: Observable[_ <: R]], Func1[_ >: Throwable, _ <: Observable[_ <: R]], Func0[_ <: Observable[_ <: R]], Int)" -> "flatMap(Int, T => Observable[R], Throwable => Observable[R], () => Observable[R])",
    "groupBy(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: R])" -> "groupBy(T => K, T => V)",
    "groupBy(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: R], Func1[Action1[K], Map[K, Object]])" -> "[TODO]",
    "mergeWith(Observable[_ <: T])" -> "merge(Observable[U])",
    "ofType(Class[R])" -> "[use `filter(_.isInstanceOf[Class])`]",
    "onBackpressureBuffer(Long, Action0)" -> "onBackpressureBuffer(Long, => Unit)",
    "onBackpressureBuffer(Long, Action0, Strategy)" -> "[TODO]",
    "onErrorResumeNext(Func1[_ >: Throwable, _ <: Observable[_ <: T]])" -> "onErrorResumeNext(Throwable => Observable[U])",
    "onErrorResumeNext(Observable[_ <: T])" -> "onErrorResumeNext(Throwable => Observable[U])",
    "onErrorReturn(Func1[_ >: Throwable, _ <: T])" -> "onErrorReturn(Throwable => U)",
    "onExceptionResumeNext(Observable[_ <: T])" -> "onExceptionResumeNext(Observable[U])",
    "publish(Func1[_ >: Observable[T], _ <: Observable[R]])" -> "publish(Observable[T] => Observable[R])",
    "reduce(Func2[T, T, T])" -> "reduce((U, U) => U)",
    "reduce(R, Func2[R, _ >: T, R])" -> "foldLeft(R)((R, T) => R)",
    "repeatWhen(Func1[_ >: Observable[_ <: Void], _ <: Observable[_]])" -> "repeatWhen(Observable[Unit] => Observable[Any])",
    "repeatWhen(Func1[_ >: Observable[_ <: Void], _ <: Observable[_]], Scheduler)" -> "repeatWhen(Observable[Unit] => Observable[Any], Scheduler)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]])" -> "replay(Observable[T] => Observable[R])",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int)" -> "replay(Observable[T] => Observable[R], Int)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Long, TimeUnit)" -> "replay(Observable[T] => Observable[R], Int, Duration)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Long, TimeUnit, Scheduler)" -> "replay(Observable[T] => Observable[R], Int, Duration, Scheduler)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Scheduler)" -> "replay(Observable[T] => Observable[R], Int, Scheduler)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Long, TimeUnit)" -> "replay(Observable[T] => Observable[R], Duration)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Long, TimeUnit, Scheduler)" -> "replay(Observable[T] => Observable[R], Duration, Scheduler)",
    "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Scheduler)" -> "replay(Observable[T] => Observable[R], Scheduler)",
    "retry(Func2[Integer, Throwable, Boolean])" -> "retry((Int, Throwable) => Boolean)",
    "retryWhen(Func1[_ >: Observable[_ <: Throwable], _ <: Observable[_]], Scheduler)" -> "retryWhen(Observable[Throwable] => Observable[Any], Scheduler)",
    "retryWhen(Func1[_ >: Observable[_ <: Throwable], _ <: Observable[_]])" -> "retryWhen(Observable[Throwable] => Observable[Any])",
    "sample(Observable[U])" -> "sample(Observable[Any])",
    "scan(Func2[T, T, T])" -> unnecessary,
    "scan(R, Func2[R, _ >: T, R])" -> "scan(R)((R, T) => R)",
    "single(Func1[_ >: T, Boolean])" -> "[use `filter(predicate).single`]",
    "singleOrDefault(T)" -> "singleOrElse(=> U)",
    "singleOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `filter(predicate).singleOrElse(default)`]",
    "skip(Int)" -> "drop(Int)",
    "skip(Long, TimeUnit)" -> "drop(Duration)",
    "skip(Long, TimeUnit, Scheduler)" -> "drop(Duration, Scheduler)",
    "skipWhile(Func1[_ >: T, Boolean])" -> "dropWhile(T => Boolean)",
    "skipUntil(Observable[U])" -> "dropUntil(Observable[Any])",
    "startWith(T)" -> "[use `item +: o`]",
    "startWith(Iterable[T])" -> "[use `Observable.from(iterable) ++ o`]",
    "startWith(Observable[T])" -> "[use `++`]",
    "skipLast(Int)" -> "dropRight(Int)",
    "skipLast(Long, TimeUnit)" -> "dropRight(Duration)",
    "skipLast(Long, TimeUnit, Scheduler)" -> "dropRight(Duration, Scheduler)",
    "subscribe()" -> "subscribe()",
    "switchIfEmpty(Observable[_ <: T])" -> "switchIfEmpty(Observable[U])",
    "switchMapDelayError(Func1[_ >: T, _ <: Observable[_ <: R]])" -> "[use `delayError.switchMap(T => Observable[R])`]",
    "takeFirst(Func1[_ >: T, Boolean])" -> "[use `filter(condition).take(1)`]",
    "takeLast(Int)" -> "takeRight(Int)",
    "takeLast(Long, TimeUnit)" -> "takeRight(Duration)",
    "takeLast(Long, TimeUnit, Scheduler)" -> "takeRight(Duration, Scheduler)",
    "takeLast(Int, Long, TimeUnit)" -> "takeRight(Int, Duration)",
    "takeLast(Int, Long, TimeUnit, Scheduler)" -> "takeRight(Int, Duration, Scheduler)",
    "takeLastBuffer(Int)" -> commentForTakeLastBuffer,
    "takeLastBuffer(Int, Long, TimeUnit)" -> commentForTakeLastBuffer,
    "takeLastBuffer(Int, Long, TimeUnit, Scheduler)" -> commentForTakeLastBuffer,
    "takeLastBuffer(Long, TimeUnit)" -> commentForTakeLastBuffer,
    "takeLastBuffer(Long, TimeUnit, Scheduler)" -> commentForTakeLastBuffer,
    "takeUntil(Observable[_ <: E])" -> "takeUntil(Observable[Any])",
    "test()" -> "[TODO]",
    "test(Long)" -> "[TODO]",
    "timeout(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(() => Observable[Any], T => Observable[Any], Observable[U])",
    "timeout(Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(T => Observable[Any], Observable[U])",
    "timeout(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]])" -> "timeout(() => Observable[Any], T => Observable[Any])",
    "timeout(Func1[_ >: T, _ <: Observable[V]])" -> "timeout(T => Observable[Any])",
    "timeout(Long, TimeUnit, Observable[_ <: T])" -> "timeout(Duration, Observable[U])",
    "timeout(Long, TimeUnit, Observable[_ <: T], Scheduler)" -> "timeout(Duration, Observable[U], Scheduler)",
    "toList()" -> "toSeq",
    "toMap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V], Func0[_ <: Map[K, V]])" -> "[mapFactory is not necessary because Scala has `CanBuildFrom`]",
    "toMultimap(Func1[_ >: T, _ <: K])" -> "toMultiMap(T => K)",
    "toMultimap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V])" -> "toMultiMap(T => K, T => V)",
    "toMultimap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V], Func0[_ <: Map[K, Collection[V]]])" -> "toMultiMap(T => K, T => V, => M)",
    "toMultimap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V], Func0[_ <: Map[K, Collection[V]]], Func1[_ >: K, _ <: Collection[V]])" -> commentForToMultimapWithCollectionFactory,
    "toSingle()" -> "[TODO]",
    "toCompletable()" -> "[TODO]",
    "toSortedList()" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)`]",
    "toSortedList(Int)" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)`]",
    "toSortedList(Func2[_ >: T, _ >: T, Integer])" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))`]",
    "toSortedList(Func2[_ >: T, _ >: T, Integer], Int)" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))`]",
    "sorted()" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)`]",
    "sorted(Func2[_ >: T, _ >: T, Integer])" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))`]",
    "window(Int)" -> "tumbling(Int)",
    "window(Int, Int)" -> "sliding(Int, Int)",
    "window(Long, TimeUnit)" -> "tumbling(Duration)",
    "window(Long, TimeUnit, Int)" -> "tumbling(Duration, Int)",
    "window(Long, TimeUnit, Int, Scheduler)" -> "tumbling(Duration, Int, Scheduler)",
    "window(Long, TimeUnit, Scheduler)" -> "tumbling(Duration, Scheduler)",
    "window(Observable[U])" -> "tumbling(=> Observable[Any])",
    "window(Func0[_ <: Observable[_ <: TClosing]])" -> "tumbling(=> Observable[Any])",
    "window(Observable[_ <: TOpening], Func1[_ >: TOpening, _ <: Observable[_ <: TClosing]])" -> "sliding(Observable[Opening])(Opening => Observable[Any])",
    "window(Long, Long, TimeUnit)" -> "sliding(Duration, Duration)",
    "window(Long, Long, TimeUnit, Scheduler)" -> "sliding(Duration, Duration, Scheduler)",
    "window(Long, Long, TimeUnit, Int, Scheduler)" -> "sliding(Duration, Duration, Int, Scheduler)",

    // manually added entries for Java static methods
    "amb(Iterable[_ <: Observable[_ <: T]])" -> "amb(Observable[T]*)",
    "create(OnSubscribe[T])" -> "apply(Subscriber[T] => Unit)",
    "combineLatest(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "combineLatestWith(Observable[U])((T, U) => R)",
    "combineLatest(List[_ <: Observable[_ <: T]], FuncN[_ <: R])" -> "combineLatest(Iterable[Observable[T]])(Seq[T] => R)",
    "combineLatest(Iterable[_ <: Observable[_ <: T]], FuncN[_ <: R])" -> "combineLatest(Iterable[Observable[T]])(Seq[T] => R)",
    "combineLatestDelayError(Iterable[_ <: Observable[_ <: T]], FuncN[_ <: R])" -> "combineLatest(Iterable[Observable[T]])(Seq[T] => R)",
    "concat(Observable[_ <: Observable[_ <: T]])" -> "concat(<:<[Observable[T], Observable[Observable[U]]])",
    "concat(Iterable[_ <: Observable[_ <: T]])" -> "[use `iter.toObservable.concat`]",
    "concatDelayError(Observable[_ <: Observable[_ <: T]])" -> "[use `delayError.concat`]",
    "concatDelayError(Iterable[_ <: Observable[_ <: T]])" -> "[use `iter.toObservable.delayError.concat`]",
    "concatEager(Observable[_ <: Observable[_ <: T]])" -> "concatEager(<:<[Observable[T], Observable[Observable[U]]])",
    "concatEager(Observable[_ <: Observable[_ <: T]], Int)" -> "concatEager(Int)(<:<[Observable[T], Observable[Observable[U]]])",
    "concatEager(Iterable[_ <: Observable[_ <: T]])" -> "[use `iter.toObservable.concatEager`]",
    "concatEager(Iterable[_ <: Observable[_ <: T]], Int)" -> "[use `iter.toObservable.concatEager(Int)`]",
    "defer(Func0[Observable[T]])" -> "defer(=> Observable[T])",
    "from(Array[T])" -> "from(Iterable[T])",
    "fromCallable(Callable[_ <: T])" -> "[use `Observable.defer(Observable.just(expensiveComputation))`]",
    "from(Iterable[_ <: T])" -> "from(Iterable[T])",
    "from(Future[_ <: T])" -> fromFuture,
    "from(Future[_ <: T], Long, TimeUnit)" -> fromFuture,
    "from(Future[_ <: T], Scheduler)" -> fromFuture,
    "fromEmitter(Action1[Emitter[T]], BackpressureMode)" -> "fromEmitter(Emitter[T] => Unit, BackpressureMode)",
    "just(T)" -> "just(T*)",
    "merge(Observable[_ <: T], Observable[_ <: T])" -> "merge(Observable[U])",
    "merge(Observable[_ <: Observable[_ <: T]])" -> "flatten(<:<[Observable[T], Observable[Observable[U]]])",
    "merge(Observable[_ <: Observable[_ <: T]], Int)" -> "flatten(Int)(<:<[Observable[T], Observable[Observable[U]]])",
    "merge(Array[Observable[_ <: T]])" -> "[use `Observable.from(array).flatten`]",
    "merge(Iterable[_ <: Observable[_ <: T]])" -> "[use `Observable.from(iter).flatten`]",
    "merge(Array[Observable[_ <: T]], Int)" -> "[use `Observable.from(array).flatten(n)`]",
    "merge(Iterable[_ <: Observable[_ <: T]], Int)" -> "[use `Observable.from(iter).flatten(n)`]",
    "mergeDelayError(Observable[_ <: T], Observable[_ <: T])" -> "[use `delayError.merge(Observable[U])`]",
    "mergeDelayError(Observable[_ <: Observable[_ <: T]])" -> "[use `delayError.flatten(<:<[Observable[T], Observable[Observable[U]]])`]",
    "mergeDelayError(Observable[_ <: Observable[_ <: T]], Int)" -> "[use `delayError.flatten(Int)(<:<[Observable[T], Observable[Observable[U]]])`]",
    "mergeDelayError(Iterable[_ <: Observable[_ <: T]])" -> "[use `iter.toObservable.delayError.flatten`]",
    "mergeDelayError(Iterable[_ <: Observable[_ <: T]], Int)" -> "[use `iter.toObservable.delayError.flatten(Int)`]",
    "sequenceEqual(Observable[_ <: T], Observable[_ <: T])" -> "sequenceEqual(Observable[U])",
    "sequenceEqual(Observable[_ <: T], Observable[_ <: T], Func2[_ >: T, _ >: T, Boolean])" -> "sequenceEqualWith(Observable[U])((U, U) => Boolean)",
    "range(Int, Int)" -> commentForRange,
    "range(Int, Int, Scheduler)" -> "[use `(start until end).toObservable.subscribeOn(scheduler)` instead of `range(start, count, scheduler)`]",
    "switchOnNext(Observable[_ <: Observable[_ <: T]])" -> "switch(<:<[Observable[T], Observable[Observable[U]]])",
    "switchOnNextDelayError(Observable[_ <: Observable[_ <: T]])" -> "[use `delayError.switch(<:<[Observable[T], Observable[Observable[U]]])`]",
    "using(Func0[Resource], Func1[_ >: Resource, _ <: Observable[_ <: T]], Action1[_ >: Resource])" -> "using(=> Resource)(Resource => Observable[T], Resource => Unit, Boolean)",
    "using(Func0[Resource], Func1[_ >: Resource, _ <: Observable[_ <: T]], Action1[_ >: Resource], Boolean)" -> "using(=> Resource)(Resource => Observable[T], Resource => Unit, Boolean)",
    "withLatestFrom(Observable[_ <: U], Func2[_ >: T, _ >: U, _ <: R])" -> "withLatestFrom(Observable[U])((T, U) => R)",
    "withLatestFrom(Array[Observable[_]], FuncN[R])" -> "[TODO]",
    "withLatestFrom(Iterable[Observable[_]], FuncN[R])" -> "[TODO]",
    "zip(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "[use instance method `zip` and `map`]",
    "zip(Observable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
    "zip(Array[Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
    "zip(Iterable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
    "zipWith(Observable[_ <: T2], Func2[_ >: T, _ >: T2, _ <: R])" -> "zipWith(Observable[U])((T, U) => R)",
    "zipWith(Iterable[_ <: T2], Func2[_ >: T, _ >: T2, _ <: R])" -> "zipWith(Iterable[U])((T, U) => R)"
  ) ++ List.iterate("T, T", 8)(s => s + ", T").map(
    // all 9 overloads of startWith:
    "startWith(" + _ + ")" -> "[use `Observable.just(...) ++ o`]"
  ).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // concat 2-9
    "concat(" + _ + ")" -> "[unnecessary because we can use `++` instead or `Observable(o1, o2, ...).concat`]"
  ).drop(1).toMap ++ List.iterate("T", 10)(s => s + ", T").map(
    // all 10 overloads of from:
    "just(" + _ + ")" -> "[use `just(T*)`]"
  ).toMap ++ (3 to 9).map(i => {
    // zip3-9:
    val obsArgs = (1 to i).map(j => s"Observable[_ <: T$j], ").mkString("")
    val funcParams = (1 to i).map(j => s"_ >: T$j, ").mkString("")
    ("zip(" + obsArgs + "Func" + i + "[" + funcParams + "_ <: R])", unnecessary)
  }).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // merge 3-9:
    "merge(" + _ + ")" -> "[unnecessary because we can use `Observable(o1, o2, ...).flatten` instead]"
  ).drop(2).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // mergeDelayError 3-9:
    "mergeDelayError(" + _ + ")" -> "[unnecessary because we can use `Observable(o1, o2, ...).flattenDelayError` instead]"
  ).drop(2).toMap ++ (3 to 9).map(i => {
    // combineLatest 3-9:
    val obsArgs = (1 to i).map(j => s"Observable[_ <: T$j], ").mkString("")
    val funcParams = (1 to i).map(j => s"_ >: T$j, ").mkString("")
    ("combineLatest(" + obsArgs + "Func" + i + "[" + funcParams + "_ <: R])", "[If C# doesn't need it, Scala doesn't need it either ;-)]")
  }).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // amb 2-9
    "amb(" + _ + ")" -> "[unnecessary because we can use `o1 amb o2` instead or `amb(List(o1, o2, o3, ...)`]"
  ).drop(1).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // concatEager 2-9
    "concatEager(" + _ + ")" -> "[unnecessary because we can use `concatEager` instead or `Observable(o1, o2, ...).concatEager`]"
  ).drop(1).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // concatDelayError 2-9
    "concatDelayError(" + _ + ")" -> "[unnecessary because we can use `Observable(o1, o2, ...).delayError.concat`]"
  ).drop(1).toMap
}
