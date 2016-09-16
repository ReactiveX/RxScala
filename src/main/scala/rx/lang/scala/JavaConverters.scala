package rx.lang.scala

import scala.language.implicitConversions
import Decorators.AsJava
import Decorators.AsScala
import Decorators.AsJavaSubscription

object JavaConverters extends DecorateAsJava with DecorateAsScala

private[scala] trait Decorators {
	class AsJava[A](op: => A) {
		def asJava: A = op
	}

	class AsJavaSubscription(s: Subscription) {
		def asJavaSubscription: rx.Subscription = s.asJavaSubscription
	}

	class AsScala[A](op: => A) {
		def asScala: A = op
	}
}

private[scala] object Decorators extends Decorators

trait DecorateAsJava {

	implicit def toJavaNotification[T](s: Notification[T]): AsJava[rx.Notification[_ <: T]] =
		new AsJava(s.asJavaNotification)

	implicit def toJavaSubscription(s: Subscription): AsJavaSubscription =
		new AsJavaSubscription(s)

	implicit def toJavaSubscriber[T](s: Subscriber[T]): AsJava[rx.Subscriber[_ >: T]] =
		new AsJava(s.asJavaSubscriber)

	implicit def toJavaScheduler(s: Scheduler): AsJava[rx.Scheduler] =
		new AsJava(s.asJavaScheduler)

	implicit def toJavaWorker(s: Worker): AsJava[rx.Scheduler.Worker] =
		new AsJava(s.asJavaWorker)

	implicit def toJavaObserver[T](s: Observer[T]): AsJava[rx.Observer[_ >: T]] =
		new AsJava(s.asJavaObserver)

	implicit def toJavaObservable[T](s: Observable[T]): AsJava[rx.Observable[_ <: T]] =
		new AsJava(s.asJavaObservable)

	implicit def toJavaOperator[T, R](operator: Subscriber[R] => Subscriber[T]): AsJava[rx.Observable.Operator[R, T]] = {
		val jOp = new rx.Observable.Operator[R, T] {
			override def call(subscriber: rx.Subscriber[_ >: R]): rx.Subscriber[_ >: T] = {
				import JavaConverters.toScalaSubscriber
				operator(subscriber.asScala).asJava
			}
		}
		new AsJava(jOp)
	}
}

trait DecorateAsScala {

	implicit def toScalaNotification[T](s: rx.Notification[_ <: T]): AsScala[Notification[T]] =
		new AsScala(Notification(s))

	implicit def toScalaSubscription(s: rx.Subscription): AsScala[Subscription] =
		new AsScala(Subscription(s))

	implicit def toScalaSubscriber[T](s: rx.Subscriber[_ >: T]): AsScala[Subscriber[T]] =
		new AsScala(Subscriber(s))

	implicit def toScalaScheduler(s: rx.Scheduler): AsScala[Scheduler] =
		new AsScala(Scheduler(s))

	implicit def toScalaWorker(s: rx.Scheduler.Worker): AsScala[Worker] =
		new AsScala(Worker(s))

	implicit def toScalaObserver[T](s: rx.Observer[_ >: T]): AsScala[Observer[T]] =
		new AsScala(Observer(s))

	implicit def toScalaObservable[T](s: rx.Observable[_ <: T]): AsScala[Observable[T]] = {
		val obs = new Observable[T] {
			val asJavaObservable: rx.Observable[_ <: T] = s
		}
		new AsScala(obs)
	}
}
