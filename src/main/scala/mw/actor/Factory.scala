package mw.actor

import scala.concurrent.ExecutionContext

trait Factory[+A <: Acting] {
  def apply(caller: Actor[Acting], exec: ExecutionContext): Actor[A]
}
