package mw.react.streams

import mw.actor.{Acting, Actor}

trait DefaultProcessor[T, S]
  extends Processor[T, S] with DefaultSubscriber[T] with DefaultPublisher[S] {
  override def subscribe(subscriber: Actor[Subscriber[S]]): Unit = {
    state match {
      case Complete => subscriber ! (_.complete())
      case Failed(error) => subscriber ! (_.fail(error))
      case _ => super.subscribe(subscriber)
    }
  }
  override protected def onComplete(subscription: Actor[Subscription]): Unit = {
    val savedSubscribers = subscribers
    subscribers = Set.empty
    super.onComplete(subscription)
    for (subscriber <- savedSubscribers) {
      subscriber ! (_.complete())
    }
  }
  override protected def onFailure(error: Throwable,
                                   subscription: Actor[Subscription],
                                   caller: Actor[Acting]): Unit = {
    val savedSubscribers = subscribers
    subscribers = Set.empty
    super.onFailure(error, subscription, caller)
    for (subscriber <- savedSubscribers) {
      subscriber ! (_.fail(error))
    }
  }
}
