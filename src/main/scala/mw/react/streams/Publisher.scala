package mw.react.streams

import mw.actor.Acting

trait Publisher[+T] {
  def subscribe(subscriber: Subscriber[T]): Unit
}
object Publisher {
  trait Implementation[T] extends Publisher[T] with Acting {
    protected var subscribers = Set.empty[Subscriber[T]]
    def subscribe(subscriber: Subscriber[T]): Unit = {
      subscribers += subscriber
      subscriber.subscribed(Subscription.inner {
        subscribers -= subscriber
      })
    }
  }
}
