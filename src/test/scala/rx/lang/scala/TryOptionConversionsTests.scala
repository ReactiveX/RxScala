package rx.lang.scala

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.observers.TestSubscriber

import scala.util.{Failure, Success}

class TryOptionConversionsTests extends JUnitSuite {

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
    val some = Some("abc")
    val observer = TestSubscriber[String]()
    some.toObservable.subscribe(observer)

    observer.assertValue("abc")
    observer.assertNoErrors()
    observer.assertCompleted()
  }

  @Test
  def testOptionNoneConversion() = {
    val some = None
    val observer = TestSubscriber[String]()
    some.toObservable.subscribe(observer)

    observer.assertNoValues()
    observer.assertNoErrors()
    observer.assertCompleted()
  }
}
