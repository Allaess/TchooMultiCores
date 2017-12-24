package mw.react.streams

import mw.actor.{Acting, Actor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S]
object Processor {
  def actor[T, S](pf: PartialFunction[T, S])
                 (implicit caller: Actor[Acting],
                  exec: ExecutionContext): Actor[Processor[T, S]] =
    Actor(Processor(pf))
  def apply[T, S](pf: PartialFunction[T, S])
                 (implicit caller: Actor[Acting], exec: ExecutionContext): Processor[T, S] =
    new Implementation(pf, caller, exec)
  class Implementation[T, S](pf: PartialFunction[T, S],
                             val creator: Actor[Acting],
                             val exec: ExecutionContext) extends DefaultProcessor[T, S] {
    protected def onNext(t: T): Unit = if (pf.isDefinedAt(t)) {
      Try(pf(t)) match {
        case Success(s) => for (subscriber <- subscribers) subscriber ! (_.next(s))
        case Failure(e) => fail(e)
      }
    }
  }
}
