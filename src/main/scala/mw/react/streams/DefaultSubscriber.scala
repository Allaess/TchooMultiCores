package mw.react.streams

import mw.actor.{Acting, Actor}

trait DefaultSubscriber[T] extends Subscriber[T] {
  protected var state: SubscriberState = Initializing
  def subscribed(subscription: Actor[Subscription]): Unit = state match {
    case Initializing => onSubscribe(subscription)
    case _ => subscription ! (_.cancel())
  }
  def next(t: T): Unit = state match {
    case Active(_) => onNext(t)
    case _ =>
  }
  def complete(): Unit = state match {
    case Active(subscription) => onComplete(subscription)
    case _ =>
  }
  override def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = state match {
    case Active(subscription) => onFailure(error, subscription, caller)
    case _ =>
  }
  protected def onSubscribe(subscription: Actor[Subscription]): Unit = {
    state = Active(subscription)
  }
  protected def onNext(t: T): Unit
  protected def onComplete(subscription: Actor[Subscription]): Unit = {
    state = Complete
    subscription ! (_.cancel())
  }
  protected def onFailure(error: Throwable,
                          subscription: Actor[Subscription],
                          caller: Actor[Acting]): Unit = {
    state = Failed(error)
    subscription ! (_.cancel())
    super.fail(error)
  }
}
