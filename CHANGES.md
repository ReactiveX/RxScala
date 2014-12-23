# RxScala Releases

## Version 0.23.1 - TODO ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22))

This release upgrades RxJava to 1.0.3 along with some enhancements and bug fixes.

### Pull Requests

* [Pull 86] (https://github.com/ReactiveX/RxScala/pull/86) Fix issue #85 that Subscription.isUnsubscribed returns a wrong value
* [Pull 92] (https://github.com/ReactiveX/RxScala/pull/92) Change the parameter name 'func' to 'f'
* [Pull 98] (https://github.com/ReactiveX/RxScala/pull/98) Add toSerialized

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22)

## Version 0.23.0 – December 4th 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22))

This release upgrades RxJava from 1.0.0-rc.5 to 1.0.2 along with some enhancements and bug fixes.

### Breaking Changes

* Breaking changes in RxJava. Read the RxJava [Release Notes](https://github.com/ReactiveX/RxScala/releases) for more information.
* Remove `Observable.compose` and `JavaConversions.toJavaTransformer` because Scala has extension methods. It's much nicer than `compose`.
* Rewrite `toMap` in an idiomatic Scala way.
 * Add `def to[M[_, _], K, V](keySelector: T => K, valueSelector: T => V)(implicit cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]): Observable[M[K, V]]`.
 * Remove `def toMap[K, V] (keySelector: T => K, valueSelector: T => V, mapFactory: () => Map[K, V]): Observable[Map[K, V]]`.
 In Scala, we can use `CanBuildFrom` to build a `Map` instead of `mapFactory`.
* Rewrite `toMultimap` in an idiomatic Scala way.
 * Change the return type from `Observable[scala.collection.Map[K, Seq[T]]]` to `Observable[mutable.MultiMap[K, V]]`.
 * Change the method name `toMultimap` to **toMultiMap** to make it consistent to the return type.
 * Remove `toMultimap(keySelector, valueSelector, mapFactory, bufferFactory)`. You can override `MultiMap.makeSet` to
  create your custom bufferFactory Instead.
  
See [RxScalaDemo.toMapExample](https://github.com/ReactiveX/RxScala/blob/a43831521b23a2f1f59e070c5addf2d41035258e/examples/src/test/scala/rx/lang/scala/examples/RxScalaDemo.scala#L982)
and [RxScalaDemo.toMultiMapExample](https://github.com/ReactiveX/RxScala/blob/a43831521b23a2f1f59e070c5addf2d41035258e/examples/src/test/scala/rx/lang/scala/examples/RxScalaDemo.scala#L1005)
for examples of new `toMap` and `toMultiMap`.

### Pull Requests

* [Pull 38] (https://github.com/ReactiveX/RxScala/pull/38) reasons why there is no Observable.range
* [Pull 45] (https://github.com/ReactiveX/RxScala/pull/45) Fail build if not all RxJava methods are mapped to a RxScala equivalent
* [Pull 46] (https://github.com/ReactiveX/RxScala/pull/46) Update to RxJava 1.0.0-RC7
* [Pull 48] (https://github.com/ReactiveX/RxScala/pull/48) Doc improvements
* [Pull 49] (https://github.com/ReactiveX/RxScala/pull/49) Update to RxJava 1.0.0-RC8
* [Pull 51] (https://github.com/ReactiveX/RxScala/pull/51) remove Observable.compose and JavaConversions.toJavaTransformer
* [Pull 52] (https://github.com/ReactiveX/RxScala/pull/52) Update to RxJava 1.0.0-RC9
* [Pull 53] (https://github.com/ReactiveX/RxScala/pull/53) Check the API coverage in examples
* [Pull 54] (https://github.com/ReactiveX/RxScala/pull/54) Convert to nanoseconds for two Duration parameters
* [Pull 56] (https://github.com/ReactiveX/RxScala/pull/56) fix link to examples
* [Pull 57] (https://github.com/ReactiveX/RxScala/pull/57) Refactor Subscriber to support backpressure when using lift and bug fixes
* [Pull 61] (https://github.com/ReactiveX/RxScala/pull/61) Update the Maven link and the Versioning section
* [Pull 64] (https://github.com/ReactiveX/RxScala/pull/64) Fix the misuse of takeWhile in demo
* [Pull 65] (https://github.com/ReactiveX/RxScala/pull/65) Update to RxJava 1.0.0-RC12
* [Pull 67] (https://github.com/ReactiveX/RxScala/pull/67) Refactor toMap
* [Pull 68] (https://github.com/ReactiveX/RxScala/pull/68) Refactor toMultimap
* [Pull 70] (https://github.com/ReactiveX/RxScala/pull/70) Fix the height issue of the img tags
* [Pull 71] (https://github.com/ReactiveX/RxScala/pull/71) Hotfix: Fix CompletenessTest

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22)

## Version 0.22.0 - October 7th 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22))

This release updates RxScala to use RxJava 1.0.0-rc.5 which included some breaking changes as it approaches 1.0 Final. Read the RxJava [Release Notes](https://github.com/ReactiveX/RxJava/releases/tag/v1.0.0-rc.5) for more information.

* [Pull 27] (https://github.com/ReactiveX/RxScala/pull/27) Upgrade to RxJava 1.0.0-RC5
* [Pull 27] (https://github.com/ReactiveX/RxScala/pull/27) Remove `groupByUntil`
* [Pull 27] (https://github.com/ReactiveX/RxScala/pull/27) Update `repeatWhen`/`retryWhen` signatures
* [Pull 29] (https://github.com/ReactiveX/RxScala/pull/29) Remove 'parallel' operator
* [Pull 23] (https://github.com/ReactiveX/RxScala/pull/23) Add missing Subject constructors and Subject classes from RxJava to RxScala
* [Pull 25] (https://github.com/ReactiveX/RxScala/pull/25) Cleanup completeness test by removing parallelMerge obsolete comparison

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22)

## Version 0.21.1 - September 28th 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22))

The first release after splitting from RxJava into its own top level project RxScala.

This is the same code as version 0.20.x except:

- all deprecated methods and types are deleted
- now published to groupId `io.reactivex` instead of `com.netflix.rxjava`
- artifactId is now `rxscala_2.10` and `rxscala_2.11` instead of `rxjava-scala`

```
io.reactivex:rxscala_2.10:0.21.1
io.reactivex:rxscala_2.11:0.21.1
```

The artifacts can be found on maven Central at: http://repo1.maven.org/maven2/io/reactivex/rxscala_2.10 and http://repo1.maven.org/maven2/io/reactivex/rxscala_2.11

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22)
