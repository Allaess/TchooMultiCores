package mw.react.streams

import mw.actor.{Acting, Actor}

trait Subscriber[-T] {
  val self: Actor[ActingSubscriber[T]]
  def subscribed(subscription: Subscription): Unit = self ! (_.subscribed(subscription))
  def next(t: T): Unit = self ! (_.next(t))
  def complete(): Unit = self ! (_.complete())
  def error(error: Throwable): Unit = self ! (_.error(error))
}
object Subscriber {
  def apply[T](action: T => Unit)(implicit caller: Actor[Acting]): Subscriber[T] = new Subscriber[T] {
    val self = Actor(ActingSubscriber(action))
  }
}
