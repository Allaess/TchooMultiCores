package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait Subscriber[-T] extends Source[T] {
  def subscribed(subscription: Subscription): Unit
}
object Subscriber {
  def apply[T, U](onNext: T => U,
                  onComplete: => Unit = {},
                  onError: Throwable => Unit = { _ => })
                 (implicit caller: Actor[Acting],
                  ctx: ExecutionContext): Subscriber[T] =
    new Subscriber[T] {
      val self = Actor.inner {
        new Subscriber[T] with Acting {
          var state: State = Active(None)
          def subscribed(subscription: Subscription): Unit = state.subscribed(subscription)
          def next(t: T): Unit = state.next(t)
          def complete(): Unit = state.complete()
          def error(throwable: Throwable): Unit = state.error(throwable)
          def exec = ctx
          def creator = caller
          sealed trait State extends Subscriber[T]
          case class Active(subscription: Option[Subscription]) extends State {
            def subscribed(subscription: Subscription): Unit = this.subscription match {
              case None =>
                state = Active(Some(subscription))
              case Some(_) =>
                subscription.cancel()
            }
            def next(t: T): Unit = onNext(t)
            def complete(): Unit = {
              state = Complete
              for (sub <- subscription) {
                sub.cancel()
              }
              onComplete
            }
            def error(throwable: Throwable): Unit = {
              state = Complete
              for (sub <- subscription) {
                sub.cancel()
              }
              onError(throwable)
              fail(throwable)
            }
          }
          case object Complete extends State {
            def subscribed(subscription: Subscription): Unit = subscription.cancel()
            def next(t: T): Unit = {}
            def complete(): Unit = {}
            def error(throwable: Throwable): Unit = {}
          }
        }
      }
      def subscribed(subscription: Subscription): Unit =
        self ! (_.subscribed(subscription))
      def next(t: T): Unit = self ! (_.next(t))
      def complete(): Unit = self ! (_.complete())
      def error(throwable: Throwable): Unit = self ! (_.error(throwable))
    }
}
