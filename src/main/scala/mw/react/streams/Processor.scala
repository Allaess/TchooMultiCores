package mw.react.streams

import mw.actor.{Acting, Actor}

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S] {
  val self: Actor[ActingProcessor[T, S]]
}
object Processor {
  def apply[T, S](pf: PartialFunction[T, S])(implicit caller: Actor[Acting]): Processor[T, S] =
    new Processor[T, S] {
      val self = Actor(ActingProcessor(pf))
    }
}