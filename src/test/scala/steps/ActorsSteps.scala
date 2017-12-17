package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ActorsSteps extends ScalaDsl with EN {
  val exec = ExecutionContext.Implicits.global
  var root: Actor[Root] = _
  var stringResult: String = _
  var myApp: Actor[MyApp] = _
  var greeter: Actor[Greeter] = _
  var futureString: Future[String] = _
  var tryString: Try[String] = _
  class MyApp(val creator: Actor[Acting])(implicit val exec: ExecutionContext) extends App {
    myApp = self
    val myActor = Actor(MyActor())
    val myGreeter = Actor(Greeter())
    def start(): Unit = stringResult = "App started"
    def tell(text: String): Unit = stringResult = text
    def askActor(): Unit = greeter = myActor ? (_.askGreeter)
    def askActing(): Unit = greeter = myActor ? (_.askActingGreeter)
    def askFuture(): Unit = futureString = myGreeter ? (_.askFuture("Marc"))
    def askString(): Unit = futureString = myGreeter ? (_.askString("Marc"))
  }
  object MyApp extends Factory[MyApp] {
    def apply(caller: Actor[Acting], ctx: ExecutionContext) = Actor(new MyApp(caller)(exec))
  }
  class MyActor(val creator: Actor[Acting])(implicit val exec: ExecutionContext)
    extends Acting {
    def askGreeter: Actor[Greeter] = Actor(Greeter())
    def askActingGreeter: Greeter = Greeter()
  }
  object MyActor {
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) = new MyActor(caller)
  }
  class Greeter(val creator: Actor[Acting])(implicit val exec: ExecutionContext)
    extends Acting {
    def identify(): Unit = stringResult = "Greeter"
    def askFuture(name: String): Future[String] = Future(s"Hello $name")
    def askString(name: String): String = s"Hello $name"
  }
  object Greeter {
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) = new Greeter(caller)
  }
  Given("""^I create a new Root Actor$""") { () =>
    root = Actor(Root(exec))
  }
  When("""^I let it start a new App$""") { () =>
    root ! (_.start(MyApp))
  }
  Then("""^the new App instance receives the start\(\) message$""") { () =>
    synchronized(wait(100))
    assert(stringResult == "App started")
  }
  When("""^I tell it something$""") { () =>
    myApp ! (_.tell("something"))
  }
  Then("""^it executes the told method$""") { () =>
    synchronized(wait(100))
    assert(stringResult == "something")
  }
  When("""^I ask it something that returns a Greeter Actor$""") { () =>
    myApp ! (_.askActor())
  }
  Then("""^I receive a Greeter Actor$""") { () =>
    synchronized(wait(100))
    greeter ! (_.identify())
    synchronized(wait(100))
    assert(stringResult == "Greeter")
  }
  When("""^I ask it something that returns a Greeter Acting$""") { () =>
    myApp ! (_.askActing())
  }
  When("""^I ask it something that returns a Future$""") { () =>
    myApp ! (_.askFuture())
  }
  Then("""^I receive a Future$""") { () =>
    implicit val ctx: ExecutionContext = exec
    synchronized(wait(100))
    futureString.onComplete(tryString = _)
    synchronized(wait(100))
    assert(tryString == Success("Hello Marc"))
  }
  When("""^I ask it something that returns anything else$""") { () =>
    myApp ! (_.askString())
  }
}
