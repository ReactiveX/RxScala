package rx.lang.scala

import rx.annotations.Experimental
import rx.functions.Cancellable

@Experimental
trait Emitter[-T] extends Observer[T] {

  private [scala] val asJavaEmitter: rx.Emitter[_ >: T] = new rx.Emitter[T] {
    import JavaConverters.toScalaSubscription

    override def requested(): Long = Emitter.this.requested
    override def setCancellation(c: rx.functions.Cancellable): Unit = Emitter.this.setCancellation(() => c.cancel())
    override def setSubscription(s: rx.Subscription): Unit = Emitter.this.setSubscription(s.asScalaSubscription)

    override def onError(e: Throwable): Unit = Emitter.this.onError(e)
    override def onCompleted(): Unit = Emitter.this.onCompleted()
    override def onNext(t: T): Unit = Emitter.this.onNext(t)
  }

  def requested: Long

  def setCancellation(onCancellation: () => Unit): Unit
  def setSubscription(subscription: Subscription): Unit
}

object Emitter {
  type BackpressureMode = rx.Emitter.BackpressureMode

  private [scala] def apply[T](emitter: rx.Emitter[T]) : Emitter[T] = new Emitter[T] {
    override val asJavaEmitter = emitter

    override def requested = emitter.requested()

    override def setCancellation(onCancellation: () => Unit): Unit = asJavaEmitter.setCancellation(new Cancellable {
      override def cancel() = onCancellation()
    })
    override def setSubscription(subscription: Subscription): Unit = asJavaEmitter.setSubscription(subscription.asJavaSubscription)

    override def onNext(value: T): Unit = asJavaEmitter.onNext(value)
    override def onError(error: Throwable): Unit = asJavaEmitter.onError(error)
    override def onCompleted(): Unit = asJavaEmitter.onCompleted()
  }
}
