package mw.react.streams

import mw.actor.Acting

trait Publisher[+T] {
  def subscribe(subscriber: Subscriber[T]): Subscription
}
object Publisher {
  trait Implementation[T] extends Publisher[T] with Acting {
    protected var subscribers = Set.empty[Subscriber[Nothing]]
    def subscribe(subscriber: Subscriber[T]) = {
      subscribers += subscriber
      Subscription {
        subscribers -= subscriber
      }
    }
  }
}
