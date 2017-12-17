package mw.actor

import scala.concurrent.ExecutionContext

trait Factory[+A <: Acting] extends ((Actor[Acting], ExecutionContext) => Actor[A])
