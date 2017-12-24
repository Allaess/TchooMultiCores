package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscription extends Acting {
  def cancel(): Unit
}
object Subscription {
  def innerActor(action: => Unit)
                (implicit caller: Actor[Acting],
                 ctx: ExecutionContext): Actor[Subscription] =
    Actor.inner(Subscription(action))
  def apply(action: => Unit)
           (implicit caller: Actor[Acting], ctx: ExecutionContext): Subscription =
    new Implementation(action, caller, ctx)
  class Implementation(action: => Unit,
                       val creator: Actor[Acting],
                       val exec: ExecutionContext)
    extends Subscription {
    def cancel(): Unit = action
  }
}
