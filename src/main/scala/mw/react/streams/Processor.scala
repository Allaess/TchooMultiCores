package mw.react.streams

import mw.actor.{Acting, Actor}
import mw.react.streams.Publisher.{ActingPublisher, PublisherActor}
import mw.react.streams.Subscriber.{ActingSubscriber, SubscriberActor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S]
object Processor {
  def apply[T, S](pf: PartialFunction[T, S])
                 (implicit caller: Actor[Acting], ctx: ExecutionContext): Processor[T, S] =
    new ProcessorActor[T, S] {
      val self = Actor(new ActingProcessor[T, S] {
        protected def onNext(t: T, subscription: Subscription): Unit = {
          if (pf.isDefinedAt(t)) Try(pf(t)) match {
            case Success(s) => for (subscriber <- subscribers) subscriber.next(s)
            case Failure(e) => error(e)
          }
        }
        def exec = ctx
        def creator = caller
      })
    }
  trait ProcessorActor[T, S]
    extends Processor[T, S] with SubscriberActor[T] with PublisherActor[S] {
    val self: Actor[Processor[T, S] with Acting]
  }
  trait ActingProcessor[T, S]
    extends Processor[T, S] with ActingSubscriber[T] with ActingPublisher[S] {
    override def onComplete(subscription: Subscription): Unit = {
      val saved = subscribers
      subscribers = Set.empty
      super.onComplete(subscription)
      for (subscriber <- saved) subscriber.complete()
    }
    override def onError(error: Throwable, subscription: Subscription): Unit = {
      val saved = subscribers
      subscribers = Set.empty
      super.onError(error, subscription)
      for (subscriber <- saved) subscriber.error(error)
    }
  }
}