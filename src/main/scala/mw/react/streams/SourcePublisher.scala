package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext

trait SourcePublisher[T] extends DefaultPublisher[T] {
  def publish(t: T): Unit = for (subscriber <- subscribers) subscriber ! (_.next(t))
}
object SourcePublisher {
  def inner[T](implicit caller: Actor[Acting],
               ctx: ExecutionContext): Actor[SourcePublisher[T]] =
    Actor.inner(SourcePublisher[T])
  def apply[T](implicit caller: Actor[Acting], ctx: ExecutionContext): SourcePublisher[T] =
    new SourcePublisher[T] {
      def creator = caller
      def exec = ctx
    }
}
