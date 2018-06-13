package rx.lang.scala

import java.util.concurrent.TimeoutException

import _root_.scala.collection.mutable
import _root_.scala.concurrent.{Future, Promise}
import _root_.scala.concurrent.duration.Duration

object ImplicitFutureConversions {

  implicit class ObservableToFuture[T](val obs: rx.lang.scala.Observable[T]) extends AnyVal {

    def toFuture()(implicit timeout: Duration): Future[Iterable[T]] = {
      val p = Promise[Iterable[T]]()
      val col = mutable.ArrayBuffer.empty[T]

      obs.timeout(timeout).subscribe (
        elem => col += elem,
        {
          case te: TimeoutException => p.success(col)
          case t => p.failure(t)
        },
        () => p.success(col)
      )

      p.future
    }

  }
}
