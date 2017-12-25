package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait Publisher[+T] {
  outer =>
  def subscribe(subscriber: Subscriber[T]): Unit
  def map[S](f: T => S)
            (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[S] = {
    val result = Processor[T, S] {
      case t => f(t)
    }
    outer.subscribe(result)
    result
  }
  def flatMap[S](f: T => Publisher[S])
                (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[S] = {
    val result = new Processor[T, S] {
      val self = Actor {
        new Processor[T, S] with Acting {
          var state: State = Active(None, Set.empty, closing = false)
          def subscribe(subscriber: Subscriber[S]): Unit = state.subscribe(subscriber)
          def subscribed(subscription: Subscription): Unit =
            state.subscribed(subscription)
          def next(t: T): Unit = state.next(t)
          def complete(): Unit = state.complete()
          def error(throwable: Throwable): Unit = state.error(throwable)
          def exec = ctx
          def creator = caller
          sealed trait State extends Processor[T, S]
          case class Active(option: Option[Subscription],
                            subscribers: Set[Subscriber[S]],
                            closing: Boolean) extends State {
            def subscribe(subscriber: Subscriber[S]): Unit =
              state = Active(option, subscribers + subscriber, closing)
            def subscribed(subscription: Subscription): Unit = option match {
              case None => state = Active(Some(subscription), subscribers, closing)
              case Some(_) => subscription.cancel()
            }
            def next(t: T): Unit = Try(f(t)) match {
              case Success(p) =>
                p.subscribe(Subscriber({ s =>
                  for (subscriber <- subscribers) {
                    subscriber.next(s)
                  }
                }, {
                  if (closing && subscribers.isEmpty) {
                    complete()
                  }
                }, { throwable =>
                  error(throwable)
                }))
              case Failure(e) =>
                error(e)
            }
            def complete(): Unit = if (subscribers.isEmpty) {
              state = Complete
              for (subscription <- option) {
                subscription.cancel()
              }
              for (subscriber <- subscribers) {
                subscriber.complete()
              }
            } else {
              state = Active(option, subscribers, closing = true)
            }
            def error(throwable: Throwable): Unit = {
              state = Error(throwable)
              for (subscription <- option) {
                subscription.cancel()
              }
              for (subscriber <- subscribers) {
                subscriber.error(throwable)
              }
              fail(throwable)
            }
          }
          case object Complete extends State {
            def subscribe(subscriber: Subscriber[S]): Unit = subscriber.complete()
            def subscribed(subscription: Subscription): Unit = subscription.cancel()
            def next(t: T): Unit = {}
            def complete(): Unit = {}
            def error(throwable: Throwable): Unit = {}
          }
          case class Error(throwable: Throwable) extends State {
            def subscribe(subscriber: Subscriber[S]): Unit = subscriber.error(throwable)
            def subscribed(subscription: Subscription): Unit = subscription.cancel()
            def next(t: T): Unit = {}
            def complete(): Unit = {}
            def error(throwable: Throwable): Unit = {}
          }
        }
      }
      def subscribe(subscriber: Subscriber[S]): Unit = self ! (_.subscribe(subscriber))
      def subscribed(subscription: Subscription): Unit =
        self ! (_.subscribed(subscription))
      def next(t: T): Unit = self ! (_.next(t))
      def complete(): Unit = self ! (_.complete())
      def error(throwable: Throwable): Unit = self ! (_.error(throwable))
    }
    outer.subscribe(result)
    result
  }
  def withFilter(p: T => Boolean)
                (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[T] = {
    val result = Processor[T, T] {
      case t if p(t) => t
    }
    outer.subscribe(result)
    result
  }
  def flatten[S](implicit id: T => Publisher[S],
                 caller: Actor[Acting],
                 ctx: ExecutionContext): Publisher[S] =
    flatMap(id)
  def collect[S](pf: PartialFunction[T, S])
                (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[S] =
    withFilter(pf.isDefinedAt).map(pf)
  def scan[S](init: S)
             (f: (S, T) => S)
             (implicit caller: Actor[Acting], ctx: ExecutionContext): Publisher[S] = {
    var s = init
    map { t =>
      s = f(s, t)
      s
    }
  }
  def foreach[U](f: T => U)(implicit caller: Actor[Acting], ctx: ExecutionContext): Unit =
    subscribe(Subscriber(f))
}
object Publisher {
  def apply[T](implicit caller: Actor[Acting], ctx: ExecutionContext):
  Publisher[T] with Source[T] =
    new Publisher[T] with Source[T] {
      val self = Actor {
        new Publisher[T] with Source[T] with Acting {
          var state: State = Active(Set.empty)
          def subscribe(subscriber: Subscriber[T]): Unit = state.subscribe(subscriber)
          def next(t: T): Unit = state.next(t)
          def complete(): Unit = state.complete()
          def error(throwable: Throwable): Unit = state.error(throwable)
          def exec = ctx
          def creator = caller
          sealed trait State extends Publisher[T] with Source[T]
          case class Active(subscribers: Set[Subscriber[T]]) extends State {
            def subscribe(subscriber: Subscriber[T]): Unit = {
              state = Active(subscribers + subscriber)
              subscriber.subscribed {
                Subscription {
                  state match {
                    case Active(subs) => state = Active(subs - subscriber)
                    case _ =>
                  }
                }
              }
            }
            def next(t: T): Unit = for (subscriber <- subscribers) {
              subscriber.next(t)
            }
            def complete(): Unit = {
              state = Complete
              for (subscriber <- subscribers) {
                subscriber.complete()
              }
            }
            def error(throwable: Throwable): Unit = {
              state = Error(throwable)
              for (subscriber <- subscribers) {
                subscriber.error(throwable)
              }
              fail(throwable)
            }
          }
          case object Complete extends State {
            def subscribe(subscriber: Subscriber[T]): Unit = subscriber.complete()
            def next(t: T): Unit = {}
            def complete(): Unit = {}
            def error(throwable: Throwable): Unit = {}
          }
          case class Error(throwable: Throwable) extends State {
            def subscribe(subscriber: Subscriber[T]): Unit = subscriber.error(throwable)
            def next(t: T): Unit = {}
            def complete(): Unit = {}
            def error(throwable: Throwable): Unit = {}
          }
        }
      }
      def subscribe(subscriber: Subscriber[T]): Unit = self ! (_.subscribe(subscriber))
      def next(t: T): Unit = self ! (_.next(t))
      def complete(): Unit = self ! (_.complete())
      def error(throwable: Throwable): Unit = self ! (_.error(throwable))
    }
}
