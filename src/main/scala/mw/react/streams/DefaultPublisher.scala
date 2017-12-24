package mw.react.streams

import mw.actor.Actor

trait DefaultPublisher[T] extends Publisher[T] {
  protected var subscribers = Set.empty[Actor[Subscriber[T]]]
  def subscribe(subscriber: Actor[Subscriber[T]]): Unit = {
    subscribers += subscriber
    subscriber ! (_.subscribed(Subscription.innerActor {
      subscribers -= subscriber
    }))
  }
}
