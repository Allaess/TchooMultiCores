package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscription {
  def cancel(): Unit
}
object Subscription {
  def apply(action: => Unit)(implicit caller: Actor[Acting], exec: ExecutionContext): Subscription =
    new Subscription {
      val self: Actor[Subscription with Acting] = Actor.inner(new Implementation(action, caller, exec))
      def cancel(): Unit = self ! (_.cancel())
    }
  class Implementation(action: => Unit, val creator: Actor[Acting], val exec: ExecutionContext)
    extends Subscription with Acting {
    def cancel(): Unit = action
  }
}
