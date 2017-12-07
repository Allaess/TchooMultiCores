package mw.react.streams

import mw.actor.{Acting, Actor}

trait ActingSubscriber[-T] extends Acting {
  protected var state: State = Initializing
  def subscribed(subscription: Subscription): Unit = state match {
    case Initializing => onSubscribe(subscription)
    case _ =>
  }
  def next(t: T): Unit = state match {
    case Active(_) => onNext(t)
    case _ =>
  }
  def complete(): Unit = state match {
    case Active(subscription) => onComplete(subscription)
    case _ =>
  }
  def error(error: Throwable): Unit = state match {
    case Active(subscription) => onError(error, subscription)
    case _ =>
  }
  protected def onSubscribe(subscription: Subscription): Unit = state = Active(subscription)
  protected def onNext(t: T): Unit
  protected def onComplete(subscription: Subscription): Unit = {
    state = Closed
    subscription.cancel()
  }
  protected def onError(error: Throwable, subscription: Subscription): Unit = {
    state = Closed
    subscription.cancel()
    super.failed(error)
  }
  override protected def failed(error: Throwable): Unit = this.error(error)
  sealed trait State
  case object Initializing extends State
  case class Active(subscription: Subscription) extends State
  case object Closed extends State
}
object ActingSubscriber {
  def apply[T](action: T => Unit)(implicit caller: Actor[Acting]): ActingSubscriber[T] =
    new ActingSubscriber[T] {
      protected def onNext(t: T): Unit = action(t)
      protected def creator = caller
    }
}
