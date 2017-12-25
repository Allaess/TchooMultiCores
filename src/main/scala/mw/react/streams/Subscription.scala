package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscription {
  def cancel(): Unit
}
object Subscription {
  def apply(action: => Unit)
           (implicit caller: Actor[Acting], ctx: ExecutionContext): Subscription =
    new Subscription {
      val self = Actor.inner {
        new Subscription with Acting {
          def cancel(): Unit = action
          def exec = ctx
          def creator = caller
        }
      }
      def cancel(): Unit = self ! (_.cancel())
    }
}
