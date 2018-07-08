package rx.lang.scala

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.observers.TestSubscriber

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ScalaTypesConversionsTests extends JUnitSuite {

  @Test
  def testIterableConversion() = {
    val it = Seq("1", "2", "3")
    val observer = TestSubscriber[String]()
    it.toObservable.subscribe(observer)

    observer.assertValues("1", "2", "3")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testIterableEmptyConversion() = {
    val it = List[String]()
    val observer = TestSubscriber[String]()
    it.toObservable.subscribe(observer)

    observer.assertNoValues()
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testTrySuccessConversion() = {
    val success = Success("abc")
    val observer = TestSubscriber[String]()
    success.toObservable.subscribe(observer)

    observer.assertValue("abc")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testTryFailureConversion() = {
    val error = new IllegalArgumentException("test error")
    val failure = Failure[String](error)
    val observer = TestSubscriber[String]()
    failure.toObservable.subscribe(observer)

    observer.assertNoValues()
    observer.assertError(error)
    observer.assertNotCompleted()
  }

  @Test
  def testOptionSomeConversion() = {
    val some = Option("abc")
    val observer = TestSubscriber[String]()
    some.toObservable.subscribe(observer)

    observer.assertValue("abc")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testOptionNoneConversion() = {
    val some = Option.empty[String]
    val observer = TestSubscriber[String]()
    some.toObservable.subscribe(observer)

    observer.assertNoValues()
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testFutureSuccessfulConversion() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = Future.successful("abc")
    val observer = TestSubscriber[String]()
    fut.toObservable.subscribe(observer)

    observer.awaitTerminalEvent()
    observer.assertValue("abc")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testFutureSuccessfulConversion2() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = Future { "abc" }
    val observer = TestSubscriber[String]()
    fut.toObservable.subscribe(observer)

    observer.awaitTerminalEvent()
    observer.assertValue("abc")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testFutureFailedConversion() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val error = new IllegalArgumentException("test error")
    val fut = Future.failed[Unit](error)
    val observer = TestSubscriber[Unit]()
    fut.toObservable.subscribe(observer)

    observer.awaitTerminalEvent()
    observer.assertNoValues()
    observer.assertError(error)
    observer.assertNotCompleted()
  }
}
