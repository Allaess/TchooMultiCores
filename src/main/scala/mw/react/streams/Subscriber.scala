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
  def apply[T](action: T => Unit)
              (implicit caller: Actor[Acting], ctx: ExecutionContext): Subscriber[T] =
    new Subscriber[T] {
      val self: Actor[Subscriber[T] with Acting] = Actor(new Implementation[T] {
        def creator = caller
        implicit def exec: ExecutionContext = ctx
        protected def onNext(t: T): Unit = action(t)
      })
      def subscribed(subscription: Subscription): Unit = self ! (_.subscribed(subscription))
      def next(t: T): Unit = self ! (_.next(t))
      def complete(): Unit = self ! (_.complete())
      def error(error: Throwable): Unit = self ! (_.error(error))
    }
  trait Implementation[T] extends Subscriber[T] with Acting {
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
      creator.failed(error)
    }
    sealed trait State
    case object Initializing extends State
    case class Active(subscription: Subscription) extends State
    case object Closed extends State
  }
}
