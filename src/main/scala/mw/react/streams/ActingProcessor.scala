package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.util.{Failure, Success, Try}

trait ActingProcessor[-T, +S] extends ActingSubscriber[T] with ActingPublisher[S]
object ActingProcessor {
  def apply[T, S](pf: PartialFunction[T, S])(implicit caller: Actor[Acting]): ActingProcessor[T, S] =
    new ActingProcessor[T, S] with ActingPublisherImplementation[S] {
      protected def onNext(t: T): Unit = if (pf.isDefinedAt(t)) {
        Try(pf(t)) match {
          case Success(s) => for (subscriber <- subscribers) subscriber.next(s)
          case Failure(e) => error(e)
        }
      }
      override protected def onComplete(subscription: Subscription): Unit = {
        val savedSubscribers = subscribers
        subscribers = Set.empty
        super.onComplete(subscription)
        for (subscriber <- savedSubscribers) subscriber.complete()
      }
      override protected def onError(error: Throwable, subscription: Subscription): Unit = {
        val savedSubscribers = subscribers
        subscribers = Set.empty
        super.onError(error, subscription)
        for (subscriber <- savedSubscribers) subscriber.error(error)
      }
      protected def creator = caller
    }
}
