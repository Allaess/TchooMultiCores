package mw.react.streams

import mw.actor.Actor

sealed trait SubscriberState
case object Initializing extends SubscriberState
case class Active(subscription: Actor[Subscription]) extends SubscriberState
case object Complete extends SubscriberState
case class Failed(error: Throwable) extends SubscriberState
