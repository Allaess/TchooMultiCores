package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait Publisher[+T] extends Acting {
  def subscribe(subscriber: Actor[Subscriber[T]]): Unit
  def map[S](f: T => S)
            (implicit caller: Actor[Acting], exec: ExecutionContext): Publisher[S] = {
    val result = Processor[T, S] {
      case t => f(t)
    }
    subscribe(Actor(result))
    result
  }
  def flatMap[S](f: T => Publisher[S])
                (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[S] = {
    val result = new DefaultProcessor[T, S] {
      processor =>
      protected var subscriptions = Set.empty[Actor[Subscription]]
      protected var closing = false
      protected def onNext(t: T): Unit = Try(f(t)) match {
        case Success(p) => Actor(p) ! (_.subscribe(Actor.inner(new DefaultSubscriber[S] {
          override protected def onSubscribe(subscription: Actor[Subscription]): Unit = {
            if (!closing || subscriptions.nonEmpty) {
              subscriptions += subscription
              super.onSubscribe(subscription)
            } else {
              subscription ! (_.cancel())
            }
          }
          protected def onNext(s: S): Unit = if (!closing || subscriptions.nonEmpty) {
            for (subscriber <- subscribers) {
              subscriber ! (_.next(s))
            }
          }
          override protected def onComplete(subscription: Actor[Subscription]): Unit = {
            subscriptions -= subscription
            super.onComplete(subscription)
            if (closing && subscriptions.isEmpty) {
              processor.complete()
            }
          }
          override protected def onFailure(error: Throwable,
                                           subscription: Actor[Subscription],
                                           caller: Actor[Acting]): Unit = {
            subscriptions -= subscription
            super.onFailure(error, subscription, caller)
            processor.fail(error)
          }
          def exec = ctx
          def creator = Actor(processor)
        })))
        case Failure(e) => fail(e)
      }
      override protected def onComplete(subscription: Actor[Subscription]): Unit = {
        closing = true
        if (subscriptions.isEmpty) super.onComplete(subscription)
      }
      override protected def onFailure(error: Throwable,
                                       subscription: Actor[Subscription],
                                       caller: Actor[Acting]): Unit = {
        val savedSubscriptions = subscriptions
        closing = true
        subscriptions = Set.empty
        super.onFailure(error, subscription, caller)
        for (subscription <- savedSubscriptions) subscription ! (_.cancel())
      }
      def exec = ctx
      def creator = caller
    }
    subscribe(Actor(result))
    result
  }
  def withFilter(p: T => Boolean)
                (implicit caller: Actor[Acting], exec: ExecutionContext): Publisher[T] = {
    val result = Processor[T, T] {
      case t if p(t) => t
    }
    subscribe(Actor(result))
    result
  }
  def flatten[S](implicit id: T => Publisher[S],
                 caller: Actor[Acting],
                 exec: ExecutionContext): Publisher[S] =
    flatMap(id)
  def collect[S](pf: PartialFunction[T, S])
                (implicit caller: Actor[Acting], exec: ExecutionContext): Publisher[S] =
    withFilter(pf.isDefinedAt).map(pf)
  def scan[S](init: S)(f: (S, T) => S): Publisher[S] = {
    var s = init
    map { t =>
      s = f(s, t)
      s
    }
  }
  def foreach[U](f: T => U): Unit = {
    subscribe(Subscriber.actor { t =>
      f(t)
    })
  }
}
