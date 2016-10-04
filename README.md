# RxScala: Reactive Extensions for Scala

This is a Scala adapter to [RxJava](http://github.com/ReactiveX/RxJava).

Example usage:

```scala
val o = Observable.interval(200 millis).take(5)
o.subscribe(n => println("n = " + n))
Observable.just(1, 2, 3, 4).reduce(_ + _)
```

For-comprehensions are also supported:

```scala
val first = Observable.just(10, 11, 12)
val second = Observable.just(10, 11, 12)
val booleans = for ((n1, n2) <- (first zip second)) yield (n1 == n2)
```

Further, this adaptor attempts to expose an API which is as Scala-idiomatic as possible. This means that certain methods have been renamed, their signature was changed, or static methods were changed to instance methods. Some examples:

```scala
 // instead of concat:
def ++[U >: T](that: Observable[U]): Observable[U]

// instance method instead of static:
def zip[U](that: Observable[U]): Observable[(T, U)] 

// the implicit evidence argument ensures that dematerialize can only be called on Observables of Notifications:
def dematerialize[U](implicit evidence: T <:< Notification[U]): Observable[U] 

// additional type parameter U with lower bound to get covariance right:
def onErrorResumeNext[U >: T](resumeFunction: Throwable => Observable[U]): Observable[U] 

// curried in Scala collections, so curry fold also here:
def fold[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] 

// using Duration instead of (long timepan, TimeUnit duration):
def sample(duration: Duration): Observable[T] 

// called skip in Java, but drop in Scala
def drop(n: Int): Observable[T] 

// there's only mapWithIndex in Java, because Java doesn't have tuples:
def zipWithIndex: Observable[(T, Int)] 

// corresponds to Java's toList:
def toSeq: Observable[Seq[T]] 

// the implicit evidence argument ensures that switch can only be called on Observables of Observables:
def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U]

// Java's from becomes apply, and we use Scala Range
def apply(range: Range): Observable[Int]

// use Bottom type:
def never: Observable[Nothing] 
```

Also, the Scala Observable is fully covariant in its type parameter, whereas the Java Observable only achieves partial covariance due to limitations of Java's type system (or if you can fix this, your suggestions are very welcome).

For more examples, see [RxScalaDemo.scala](https://github.com/ReactiveX/RxScala/blob/0.x/examples/src/test/scala/examples/RxScalaDemo.scala).

Scala code using Rx should only import members from `rx.lang.scala` and below.


## Master Build Status

<a href='https://travis-ci.org/ReactiveX/RxScala/builds'><img src='https://travis-ci.org/ReactiveX/RxScala.svg?branch=0.x'></a>

## Communication

Since RxScala is part of the RxJava family the communication channels are similar:

- Google Group: [RxJava](http://groups.google.com/d/forum/rxjava)
- Twitter: [@RxJava](http://twitter.com/RxJava)
- [GitHub Issues](https://github.com/ReactiveX/RxScala/issues)

## Versioning

| RxScala version | Compatible RxJava version |
| ------------------- | ------------------------- |
| 0.26.* | 1.0.* |
| 0.25.* | 1.0.* |
| 0.24.* | 1.0.* |
| 0.23.*<sup>[1]</sup> | 1.0.* |
| 0.22.0 | 1.0.0-rc.5 |
| 0.21.1 | 1.0.0-rc.3 |
| 0.X.Y (X < 21)<sup>[2]</sup> | 0.X.Y |

[1] You can use any release of RxScala 0.23 with any release of RxJava 1.0. E.g, use RxScala 0.23.0 with RxJava 1.0.1 <br/>
[2] You should use the same version of RxScala with RxJava. E.g, use RxScala 0.20.1 with RxJava 0.20.1

If you are using APIs labeled with `Experimental/Beta`, or `ExperimentalAPIs` (deprecated since 0.25.0), which uses RxJava Beta/Experimental APIs,
you should use the corresponding version of RxJava as the following table:

| RxScala version | Compatible RxJava version |
| ------------------- | ------------------------- |
| 0.26.3 | 1.2.0+ |
| 0.26.2 | 1.1.6+ |
| 0.26.1 | 1.1.1+ |
| 0.26.0 | 1.1.0+ |
| 0.25.1 | 1.0.17+ |
| 0.25.0 | 1.0.11+ |
| 0.24.1 | 1.0.8+ |
| 0.24.0 | 1.0.7+ |

## Full Documentation

RxScala: 

- The API documentation can be found [here](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Observable). 

Note that starting from version 0.15, `rx.lang.scala.Observable` is not a value class any more.  [./Rationale.md](https://github.com/ReactiveX/RxScala/blob/0.x/Rationale.md) explains why.

You can build the API documentation yourself by running `sbt doc` in the RxScala root directory. Open `target/scala-2.11/api/index.html` to display it.

RxJava:

- [Wiki](https://github.com/ReactiveX/RxJava/wiki)
- [Javadoc](http://reactivex.io/RxJava/javadoc/)

## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7C%22rxscala%22%20AND%20g%3A%22io.reactivex%22).

Example for sbt/activator:

```scala
libraryDependencies += "io.reactivex" %% "rxscala" % "x.y.z"
```

and for Maven:

```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxscala_${scala.compat.version}</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="io.reactivex" name="rxscala_${scala.compat.version}" rev="x.y.z" />
```

## Build

To build you need [sbt](http://scala-sbt.org):

```
$ git clone git@github.com:ReactiveX/RxScala.git
$ cd RxScala
$ TRAVIS_TAG=1.0.0-RC1 sbt package
```

Use `TRAVIS_TAG` to set the version of the package.

You can also run the examples from within `sbt`:

```
$ sbt examples/run
```

When you see the list of available `App` objects pick the one you want to execute.

```
Multiple main classes detected, select one to run:

 [1] AsyncWikiErrorHandling
 [2] SyncObservable
 [3] AsyncObservable
 [4] AsyncWiki
 [5] Transforming

Enter number:
```

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxScala/issues).

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


