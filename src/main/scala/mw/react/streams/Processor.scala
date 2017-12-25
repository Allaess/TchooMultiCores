package mw.react.streams

import mw.actor
import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S]
object Processor {
  def apply[T, S](pf: PartialFunction[T, S])
                 (implicit caller: Actor[Acting], ctx: ExecutionContext): Processor[T, S] =
    new Processor[T, S] {
      val self = actor.Actor {
        new Processor[T, S] with Acting {
          var state: State = Active(None, Set.empty)
          def subscribe(subscriber: Subscriber[S]): Unit = state.subscribe(subscriber)
          def subscribed(subscription: Subscription): Unit = state.subscribed(subscription)
          def next(t: T): Unit = state.next(t)
          def complete(): Unit = state.complete()
          def error(throwable: Throwable): Unit = state.error(throwable)
          def exec = ctx
          def creator = caller
          sealed trait State extends Processor[T, S]
          case class Active(option: Option[Subscription], subscribers: Set[Subscriber[S]])
            extends State {
            def subscribe(subscriber: Subscriber[S]): Unit =
              state = Active(option, subscribers + subscriber)
            def subscribed(subscription: Subscription): Unit = option match {
              case None =>
                state = Active(Some(subscription), subscribers)
              case Some(_) =>
                subscription.cancel()
            }
            def next(t: T): Unit = try {
              if (pf.isDefinedAt(t)) {
                val s = pf(t)
                for (subscriber <- subscribers) {
                  subscriber.next(s)
                }
              }
            } catch {
              case throwable: Throwable => error(throwable)
            }
            def complete(): Unit = {
              state = Complete
              for (subscription <- option) {
                subscription.cancel()
              }
              for (subscriber <- subscribers) {
                subscriber.complete()
              }
            }
            def error(throwable: Throwable): Unit = {
              state = Error(throwable)
              for (subscription <- option) {
                subscription.cancel()
              }
              for (subscriber <- subscribers) {
                subscriber.complete()
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
}
