package mw.react.streams

import mw.actor.{Acting, Actor}

trait Subscriber[-T] extends Acting {
  def next(t: T): Unit
  def complete(): Unit
  def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit
}
