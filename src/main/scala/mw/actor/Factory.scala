package mw.actor

trait Factory[+A <: Acting] {
  type Args >: Nothing
  def apply(args: Args)(implicit creator: Actor[Acting]): Actor[A]
}
