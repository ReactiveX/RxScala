package examples

import java.util.concurrent.CountDownLatch

import org.junit.{Ignore, Test}
import org.scalatest.junit.JUnitSuite
import rx.functions.Action1
import rx.lang.scala.JavaConverters._
import rx.lang.scala.{Observable, Observer, Subscriber, Subscription}

import scala.language.postfixOps

@Ignore // Since this doesn't do automatic testing, don't increase build time unnecessarily
class JavaConvertersDemo extends JUnitSuite {

  @Test
  def javaObservableToScalaObservableConverter() = {

    // given a RxJava Observable (for example from some third party library)...
    def getJavaObservableFromSomewhere: rx.Observable[Int] =
      rx.Observable.just(1, 2, 3)

    // it is possible to transform this into a RxScala Observable using `asScala`
    // after that you can use all Scala style operators
    getJavaObservableFromSomewhere.asScala
      .map(2 *)
      .filter(_ % 4 == 0)
      .subscribe(println, _.printStackTrace(), () => println("done"))
  }

  @Test
  def scalaObservableToJavaObservableConverter() = {

    // given a function that takes a RxJava Observable as its input and returns a RxJava Subscription ...
    def useJavaObservableSomewhere(obs: rx.Observable[_ <: Int]): rx.Subscription =
      obs.subscribe(new Action1[Int] { override def call(i: Int): Unit = println(i) })

    // you can give a RxScala Observable as input and call `asJava`on it...
    val javaSubscription: rx.Subscription = useJavaObservableSomewhere(Observable.just(1, 2, 3).asJava)

    // after which you get a RxJava Subscription back, which can be used again in a RxScala setting using `asScalaSubscription`...
    val scalaSubscription: Subscription = javaSubscription.asScalaSubscription

    // and convert it back to a RxJava Subscription using `asJavaSubscription`
    val javaSubscriptionAgain: rx.Subscription = scalaSubscription.asJavaSubscription
  }

  @Test
  def schedulerConverting() = {
    // the next line is not part of the actual example, but is just to prevent early termination due to scheduling
    val latch = new CountDownLatch(1)


    // given a function that return a particular RxJava-specific scheduler (for example the
    // JavaFxScheduler from RxJavaFx (https://github.com/ReactiveX/RxJavaFX))...
    def getJavaSpecificScheduler: rx.Scheduler =
      rx.schedulers.Schedulers.computation()

    Observable.just(1, 2, 3)
      .doOnNext(_ => println(Thread.currentThread().getName)) // should print "main"
      .observeOn(getJavaSpecificScheduler.asScala)
      .doOnNext(_ => println(Thread.currentThread().getName)) // should print the name of the scheduler as its side effect
      .subscribe(println, e => { e.printStackTrace(); latch.countDown() }, () => { println("done"); latch.countDown() })


    latch.await()
  }

  def observableCreate(): Unit = {
    Observable((subscriber: Subscriber[Int]) => {
      // As a subscriber is both a Subscriber, Observer and Subscription, you need some way to distinguish between them
      // Observer uses the regular `asJava` and `asScala` operators;
      // Subscriber uses `asJavaSubscriber` and `asScalaSubscriber`;
      // Subscription uses the `asJavaSubscription` and `asScalaSubscription` instead.
      val jObserver: rx.Observer[_ >: Int] = subscriber.asJava
      val jSubscriber: rx.Subscriber[_ >: Int] = subscriber.asJavaSubscriber
      val jSubscription: rx.Subscription = subscriber.asJavaSubscription

      val sObserver: Observer[_ >: Int] = jObserver.asScala
      val sSubscriber: Subscriber[_ >: Int] = jSubscriber.asScalaSubscriber
      val sSubscription: Subscription = jSubscription.asScalaSubscription
    })
  }
}
