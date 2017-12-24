package mw.actor

import scala.concurrent.ExecutionContext

class Root(implicit val exec: ExecutionContext) extends Acting {
  private var factories = Map.empty[Actor[Acting], Factory[Application]]
  def creator = self
  def start[A <: Application](implicit factory: Factory[A]): Unit = {
    val app = factory(self, exec)
    factories += app -> factory
    app ! (_.start())
  }
  override def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = {
    val message = error.getMessage
    System.err.println(s"Application $caller failed: $message")
    error.printStackTrace()
    factories.get(caller) match {
      case Some(factory) =>
        System.err.println(s"Restarting $caller")
        factories -= caller
        start(factory)
      case None =>
        System.err.println(s"Cannot restart $caller")
    }
  }
}
object Root {
  def apply(implicit exec: ExecutionContext) = new Root
}
