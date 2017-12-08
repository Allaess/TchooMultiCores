package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S]
object Processor {
  def apply[T, S](pf: PartialFunction[T, S])
                 (implicit caller: Actor[Acting], ctx: ExecutionContext): Processor[T, S] =
    new Processor[T, S] {
      val self: Actor[Processor[T, S] with Acting] = Actor(new Implementation[T, S] {
        def creator = caller
        def exec: ExecutionContext = ctx
        protected def onNext(t: T): Unit = if (pf.isDefinedAt(t)) Try(pf(t)) match {
          case Success(s) => for (subscriber <- subscribers) subscriber.next(s)
          case Failure(e) => failed(e)
        }
      })
      def subscribe(subscriber: Subscriber[S]): Unit = self ! (_.subscribe(subscriber))
      def subscribed(subscription: Subscription): Unit = self ! (_.subscribed(subscription))
      def next(t: T): Unit = self ! (_.next(t))
      def complete(): Unit = self ! (_.complete())
      def error(error: Throwable): Unit = self ! (_.error(error))
    }
  trait Implementation[T, S] extends Processor[T, S]
    with Subscriber.Implementation[T]
    with Publisher.Implementation[S] {
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
  }
}
