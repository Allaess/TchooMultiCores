package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscriber[-T] extends Acting {
  def subscribed(subscription: Actor[Subscription]): Unit
  def next(t: T): Unit
  def complete(): Unit
}
object Subscriber {
  def actor[T](action: T => Unit)
              (implicit caller: Actor[Acting],
               ctx: ExecutionContext): Actor[Subscriber[T]] = {
    Actor(Subscriber(action))
  }
  def apply[T](action: T => Unit)
              (implicit caller: Actor[Acting], ctx: ExecutionContext): Subscriber[T] =
    new Implementation[T](action, caller, ctx)
  class Implementation[T](action: T => Unit,
                          val creator: Actor[Acting],
                          val exec: ExecutionContext)
    extends DefaultSubscriber[T] {
    protected def onNext(t: T): Unit = action(t)
  }
}
