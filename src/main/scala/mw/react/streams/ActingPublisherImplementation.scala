package mw.react.streams

trait ActingPublisherImplementation[T] extends ActingPublisher[T] {
  protected var subscribers = Set.empty[Subscriber[T]]
  def subscribe(subscriber: Subscriber[T]): Unit = {
    subscribers += subscriber
    subscriber.subscribed(Subscription {
      subscribers -= subscriber
    })
  }
}
