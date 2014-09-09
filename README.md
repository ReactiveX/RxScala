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

For more examples, see [RxScalaDemo.scala](https://github.com/ReactiveX/RxScala/blob/0.x/src/examples/scala/rx/lang/scala/examples/RxScalaDemo.scala).

Scala code using Rx should only import members from `rx.lang.scala` and below.


## Master Build Status

<a href='https://travis-ci.org/ReactiveX/RxScala/builds'><img src='https://travis-ci.org/ReactiveX/RxScala.svg?branch=0.x'></a>

## Communication

Since RxScala is part of the RxJava family the communication channels are similar:

- Google Group: [RxJava](http://groups.google.com/d/forum/rxjava)
- Twitter: [@RxJava](http://twitter.com/RxJava)
- [GitHub Issues](https://github.com/ReactiveX/RxScala/issues)

## Versioning

RxScala 0.x is based on RxScala 0.x. RxScala 1.0 will be released when RxScala 1.0 is released.

As of 1.0.0 semantic versioning will be used.

## Full Documentation

RxScala: 

- The API documentation can be found [here](http://rxscala.github.io/scaladoc/index.html#rx.lang.scala.Observable). 

Note that starting from version 0.15, `rx.lang.scala.Observable` is not a value class any more.  [./Rationale.md](https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/Rationale.md) explains why.

You can build the API documentation yourself by running `./gradlew scaladoc` in the RxJava root directory.

Then navigate to `RxJava/language-adaptors/rxjava-scala/build/docs/scaladoc/index.html` to display it.

RxJava:

- [Wiki](https://github.com/ReactiveX/RxJava/wiki)
- [Javadoc](http://reactivex.io/RxJava/javadoc/)



## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-scala%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-scala</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-scala" rev="x.y.z" />
```

and for sbt:

```scala
libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "x.y.z"
)
```

## Build

To build:

```
$ git clone git@github.com:ReactiveX/RxScala.git
$ cd RxScala/
$ ./RxScala build
```

Futher details on building can be found on the RxJava [Getting Started](https://github.com/ReactiveX/RxJava/wiki/Getting-Started) page of the wiki.

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxGroovy/issues).


## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


