package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscriber[-T] {
  def subscribed(subscription: Subscription): Unit
  def next(t: T): Unit
  def complete(): Unit
  def error(error: Throwable): Unit
}
object Subscriber {
  def apply[T](action: (T, Subscription) => Unit)
              (implicit caller: Actor[Acting], ctx: ExecutionContext): Subscriber[T] =
    new SubscriberActor[T] {
      val self = Actor(new ActingSubscriber[T] {
        protected def onNext(t: T, subscription: Subscription): Unit = action(t, subscription)
        def exec = ctx
        def creator = caller
      })
    }
  trait SubscriberActor[T] extends Subscriber[T] {
    val self: Actor[Subscriber[T] with Acting]
    def subscribed(subscription: Subscription): Unit = self ! (_.subscribed(subscription))
    def next(t: T): Unit = self ! (_.next(t))
    def complete(): Unit = self ! (_.complete())
    def error(error: Throwable): Unit = self ! (_.error(error))
  }
  trait ActingSubscriber[T] extends Subscriber[T] with Acting {
    protected var state: State = Initializing
    def subscribed(subscription: Subscription): Unit = state match {
      case Initializing => onSubscribe(subscription)
      case _ => subscription.cancel()
    }
    def next(t: T): Unit = state match {
      case Active(subscription) => onNext(t, subscription)
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
    protected def onSubscribe(subscription: Subscription): Unit = {
      state = Active(subscription)
    }
    protected def onNext(t: T, subscription: Subscription): Unit
    protected def onComplete(subscription: Subscription): Unit = {
      state = Complete
      subscription.cancel()
    }
    protected def onError(error: Throwable, subscription: Subscription): Unit = {
      state = Failed(error)
      subscription.cancel()
    }
  }
  sealed trait State
  case object Initializing extends State
  case class Active(subscription: Subscription) extends State
  case object Complete extends State
  case class Failed(error: Throwable) extends State
}
