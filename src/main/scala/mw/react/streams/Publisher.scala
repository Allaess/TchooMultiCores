package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.util.{Failure, Success, Try}

trait Publisher[+T] {
  outer =>
  val self: Actor[ActingPublisher[T]]
  def subscribe(subscriber: Subscriber[T]): Unit = self ! (_.subscribe(subscriber))
  def map[S](f: T => S)(implicit caller: Actor[Acting]): Publisher[S] = {
    val result = Processor[T, S] { case t => f(t) }
    outer.subscribe(result)
    result
  }
  def flatMap[S](f: T => Publisher[S])(implicit caller: Actor[Acting]): Publisher[S] = {
    val result = new Processor[T, S] {
      val self = Actor(new ActingProcessor[T, S] with ActingPublisherImplementation[S] {
        processor =>
        protected var subscriptions = Set.empty[Subscription]
        protected var closing = false
        protected def onNext(t: T): Unit = Try(f(t)) match {
          case Success(p) => p.subscribe(new Subscriber[S] {
            val self = InnerActor(new ActingSubscriber[S] {
              override protected def onSubscribe(subscription: Subscription): Unit = {
                subscriptions += subscription
                super.onSubscribe(subscription)
              }
              protected def onNext(s: S): Unit = for (subscriber <- subscribers) subscriber.next(s)
              override protected def onComplete(subscription: Subscription): Unit = {
                subscriptions -= subscription
                super.onComplete(subscription)
                if (closing && subscriptions.isEmpty) processor.complete()
              }
              override protected def onError(error: Throwable, subscription: Subscription): Unit = {
                processor.error(error)
              }
              protected def creator = Actor(processor)
            })
          })
          case Failure(e) => error(e)
        }
        override protected def onComplete(subscription: Subscription): Unit = {
          closing = true
          if (subscriptions.isEmpty) super.onComplete(subscription)
        }
        override protected def onError(error: Throwable, subscription: Subscription): Unit = {
          val savedSubscriptions = subscriptions
          subscriptions = Set.empty
          closing = true
          super.onError(error, subscription)
          for (subscription <- savedSubscriptions) subscription.cancel()
        }
        protected def creator = caller
      })
    }
    outer.subscribe(result)
    result
  }
  def withFilter(p: T => Boolean)(implicit caller: Actor[Acting]): Publisher[T] = {
    val result = Processor[T, T] { case t if p(t) => t }
    outer.subscribe(result)
    result
  }
  def flatten[S](implicit id: T => Publisher[S], caller: Actor[Acting]): Publisher[S] = flatMap(id)
  def collect[S](pf: PartialFunction[T, S])(implicit caller: Actor[Acting]): Publisher[S] =
    withFilter(pf.isDefinedAt).map(pf)
  def scan[S](init: S)(f: (S, T) => S)(implicit caller: Actor[Acting]): Publisher[S] = {
    var s = init
    val result = Processor[T, S] { case t =>
      s = f(s, t)
      s
    }
    outer.subscribe(result)
    result
  }
  def foreach[U](f: T => U)(implicit caller: Actor[Acting]): Unit =
    outer.subscribe(Subscriber[T] { t => f(t) })
}
