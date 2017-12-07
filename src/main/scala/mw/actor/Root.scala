package mw.actor

class Root extends Acting {
  private var factories = Map.empty[Actor[Acting], Actor[Acting] => Actor[Application]]
  def creator = self
  def start[A <: Application](factory: Factory[A])(args: factory.Args): Unit = start(factory(args)(_))
  protected def start[A <: Application](factory: Actor[Acting] => Actor[Application]): Unit = {
    val app = factory(self)
    factories += app -> factory
    app ! (_.start())
  }
  override def failed(actor: Actor[Acting], error: Throwable): Unit = {
    System.err.println(s"Actor $actor failed with message: ${error.getMessage}")
    error.printStackTrace()
    factories.get(actor) match {
      case Some(factory) =>
        factories -= actor
        System.err.println("Restarting...")
        start(factory)
      case None =>
        factories -= actor
        System.err.println("Cannot restart")
    }
  }
  override def failed(error: Throwable): Unit = {
    System.err.println(s"System failure: ${error.getMessage}")
    error.printStackTrace()
    System.exit(1)
  }
}
