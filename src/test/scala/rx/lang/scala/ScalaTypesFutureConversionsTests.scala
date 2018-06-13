package rx.lang.scala

import org.junit.Test
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.Waiters
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.observers.TestSubscriber
import rx.lang.scala.ImplicitFutureConversions._
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration.Duration

class ScalaTypesFutureConversionsTests extends JUnitSuite with ScalaFutures with Waiters {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val defaultEventWaitTimeout: Duration = defaultPatience.timeout

  @Test
  def testIterableConversion(): Unit = {
    val it = Seq("1", "2", "3")
    val f = it.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == it)
    }
  }

  @Test
  def testIterableEmptyConversion(): Unit = {
    val it = List[String]()
    val f = it.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == it)
    }
  }

  @Test
  def testTrySuccessConversion(): Unit = {
    val success = Success("abc")
    val f = success.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == Seq("abc"))
    }
  }

  @Test
  def testTryFailureConversion(): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    val error = new IllegalArgumentException("test error")
    val failure = Failure[String](error)
    val w = new Waiter
    val f = failure.toObservable.toFuture

    f onComplete {
      case Failure(e) => w(throw e); w.dismiss()
      case Success(_) => w.dismiss()
    }

    intercept[IllegalArgumentException] {
      w.await
    }
  }

  @Test
  def testOptionSomeConversion(): Unit = {
    val some = Option("abc")
    val f = some.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == Seq("abc"))
    }
  }

  @Test
  def testOptionNoneConversion(): Unit = {
    val none = Option.empty[String]
    val f = none.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == Seq.empty[String])
    }
  }

  @Test
  def testFutureSuccessfulConversion(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = Future.successful("abc")
    val f = fut.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == Seq("abc"))
    }
  }

  @Test
  def testFutureSuccessfulConversion2(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = Future { "abc" }
    val f = fut.toObservable.toFuture

    whenReady(f) { result =>
      assert(result == Seq("abc"))
    }
  }

  @Test
  def testFutureFailedConversion(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val error = new IllegalArgumentException("test error")
    val fut = Future.failed[Unit](error)
    val w = new Waiter
    val f = fut.toObservable.toFuture

    f onComplete {
      case Failure(e) => w(throw e); w.dismiss()
      case Success(_) => w.dismiss()
    }

    intercept[IllegalArgumentException] {
      w.await
    }
  }
}
