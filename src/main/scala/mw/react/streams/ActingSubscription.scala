package mw.react.streams

import mw.actor.{Acting, Actor}

trait ActingSubscription extends Acting {
  def cancel(): Unit
}
object ActingSubscription {
  def apply(action: => Unit)(implicit caller: Actor[Acting]): ActingSubscription =
    new ActingSubscription {
      def cancel(): Unit = action
      protected def creator = caller
    }
}