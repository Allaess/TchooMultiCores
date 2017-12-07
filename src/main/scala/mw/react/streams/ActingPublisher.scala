package mw.react.streams

import mw.actor.{Acting, Actor}

trait ActingPublisher[+T] extends Acting {
  def subscribe(subscriber: Subscriber[T]): Unit
  object Subscription {
    def apply(action: => Unit)(implicit caller: Actor[Acting]): Subscription = new Subscription {
      val self = InnerActor(ActingSubscription(action))
    }
  }
  object Subscriber {
    def apply[S](action: S => Unit)(implicit caller: Actor[Acting]): Subscriber[S] =
      new Subscriber[S] {
        val self = InnerActor(ActingSubscriber(action))
      }
  }
}
