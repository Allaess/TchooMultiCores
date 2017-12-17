package mw.react.streams

import mw.actor.{Acting, Actor}
import mw.react.streams.Publisher.{ActingPublisher, PublisherActor}

import scala.concurrent.ExecutionContext

trait Publisher[+T] {
  outer =>
  def subscribe(subscriber: Subscriber[T]): Unit
  def map[S](f: T => S): Publisher[S] = {
    val result = Processor {
      case t => f(t)
    }
    outer.subscribe(result)
    result
  }
  def flatMap[S](f: T => Publisher[S]): Publisher[S] = new PublisherActor[S] {
    val self = Actor(new ActingPublisher[S]{
      implicit def exec: ExecutionContext = ???
      def creator = ???
    })
  }
  def withFilter(p: T => Boolean): Publisher[T] = {
    val result = Processor {
      case t if p(t) => t
    }
    outer.subscribe(result)
    result
  }
  def flatten[S](implicit id: T => Publisher[S]): Publisher[S] = flatMap(id)
  def collect[S](pf: PartialFunction[T, S]): Publisher[S] = withFilter(pf.isDefinedAt).map(pf)
  def scan[S](init: S)(f: (S, T) => S): Publisher[S] = {
    var s = init
    map { t =>
      s = f(s, t)
      s
    }
  }
}
object Publisher {
  trait PublisherActor[T] extends Publisher[T] {
    val self: Actor[Publisher[T] with Acting]
    def subscribe(subscriber: Subscriber[T]): Unit = self ! (_.subscribe(subscriber))
  }
  trait ActingPublisher[T] extends Publisher[T] with Acting {
    outer =>
    protected var subscribers = Set.empty[Subscriber[T]]
    def subscribe(subscriber: Subscriber[T]): Unit = {
      subscribers += subscriber
      Actor.inner(new Subscription with Acting {
        def cancel(): Unit = subscribers -= subscriber
        def exec = outer.exec
        def creator = Actor(outer)
      })
    }
  }
}
