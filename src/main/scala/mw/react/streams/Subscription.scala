package mw.react.streams

import mw.actor.Actor

trait Subscription {
  val self: Actor[ActingSubscription]
  def cancel(): Unit = self ! (_.cancel())
}
