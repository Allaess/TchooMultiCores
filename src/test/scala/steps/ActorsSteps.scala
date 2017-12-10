package steps

import cucumber.api.PendingException
import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor.{Acting, Actor}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ActorsSteps extends ScalaDsl with EN {
  object Lazies {
    implicit val exec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    lazy val app = Actor(MyApp())
    lazy val app2 = Actor(MyApp())
    implicit val caller: Actor[Acting] = app
    lazy val anotherActor = Actor(MyActing())
    lazy val failedActor = app.withFilter(_ => false)
    lazy val successActor = app.withFilter(_ => true)
    lazy val forComprehensionResult = for (acting <- app) yield f(acting)
    lazy val forComp2Result = for {
      a <- app
      b <- anotherActor
    } yield {
      g(a, b)
    }
    lazy val actorResult = app.?(_.self)
    lazy val anotherActorResult = app.?(_.self)
  }
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  var tellArg: String = _
  var futureResult: Future[String] = _
  class MyApp(implicit val exec: ExecutionContext) extends Acting {
    def creator = self
    def tell(arg: String): Unit = tellArg = arg
    def greet(name: String): String = s"Hello $name"
    def askGreet(name: String): Future[String] = self ? (_.greet(name))
  }
  object MyApp {
    def apply()(implicit exec: ExecutionContext) = new MyApp
  }
  class MyActing(val creator: Actor[Acting], val exec: ExecutionContext) extends Acting {
    def identify: String = "MyActing"
  }
  object MyActing {
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) = new MyActing(caller, exec)
  }
  class MyOtherActing(val creator: Actor[Acting], val exec: ExecutionContext) extends Acting
  object MyOtherActing {
    def identify: String = "MyOtherActing"
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) =
      new MyOtherActing(caller, exec)
  }
  def f(app: MyApp)(implicit caller: Actor[Acting], exec: ExecutionContext): MyActing = MyActing()
  def g(app: MyApp, act: MyActing)
       (implicit caller: Actor[Acting], exec: ExecutionContext): MyOtherActing =
    MyOtherActing()
  Given("""^I instantiate an actor$""") { () =>
    Lazies.app
    tellArg = null
    futureResult = null
  }
  When("""^I tell it something$""") { () =>
    Lazies.app ! (_.tell("Something"))
  }
  Then("""^the corresponding method is executed$""") { () =>
    synchronized {
      wait(100)
    }
    assert(tellArg == "Something")
  }
  When("""^I ask it something that returns a Future$""") { () =>
    futureResult = Lazies.app ? (_.askGreet("Marc"))
  }
  Then("""^the returned future will contain the answer$""") { () =>
    synchronized {
      wait(100)
    }
    assert(futureResult.value.get.get == "Hello Marc")
  }
  When("""^I ask it something that returns an Actor$""") { () =>
    Lazies.actorResult
  }
  Then("""^the returned actor will contain the answer$""") { () =>
    val t1 = Lazies.app ? (_.toString)
    val t2 = Lazies.actorResult ? (_.toString)
    synchronized {
      wait(100)
    }
    assert(t1.value.get.get == t2.value.get.get)
  }
  When("""^I ask it something that returns anything else$""") { () =>
    futureResult = Lazies.app ? (_.greet("Marc"))
  }
  Given("""^I make it fail$""") { () =>
    Lazies.app ! { _ =>
      throw new Exception("Intended failure")
    }
  }
  Then("""^nothing happens$""") { () =>
    synchronized {
      wait(100)
    }
    assert(tellArg == null)
  }
  Then("""^it returns a failed Future$""") { () =>
    synchronized {
      wait(100)
    }
    assert(futureResult.value.get.isFailure)
  }
  Then("""^it returns a failed Actor$""") { () =>
    synchronized {
      wait(100)
    }
    assert(Lazies.actorResult.isFailed)
  }
  When("""^I ask it again something that returns an Actor$""") { () =>
    Lazies.anotherActorResult
  }
  Then("""^it returns another failed Actor$""") { () =>
    assert(Lazies.anotherActorResult.isFailed)
  }
  When("""^I use a for comprehension over it$""") { () =>
    Lazies.forComprehensionResult
  }
  Then("""^I get an Actor corresponding to the mapped function$""") { () =>
    val future = Lazies.forComprehensionResult ? (_.identify)
    var result = Try("")
    implicit val exec: ExecutionContext = Lazies.exec
    future.onComplete(result = _)
    synchronized {
      wait(100)
    }
    assert(result == Success("MyActing"))
  }
  Given("""^I instantiate another Actor$""") { () =>
    Lazies.anotherActor
  }
  When("""^I use a for comprehension over both Actors$""") { () =>
    Lazies.forComp2Result
  }
  Then("""^I get an Actor corresponding to the map and flatMap functions$""") { () =>
    var result: Boolean = false
    Lazies.forComp2Result ! { acting =>
      result = acting.isInstanceOf[MyOtherActing]
    }
    synchronized {
      wait(100)
    }
    assert(result)
  }
  When("""^I filter it with a failed condition$""") { () =>
    Lazies.failedActor
  }
  Then("""^I get an empty Actor back$""") { () =>
    assert(Lazies.failedActor.getClass == Actor.empty.getClass)
  }
  When("""^I filter it with a successful condition$""") { () =>
    Lazies.successActor
  }
  Then("""^I get the same Actor back$""") { () =>
    assert(Lazies.app == Lazies.successActor)
  }
  When("""^I iterate over it$""") { () =>
    //// Write code here that turns the phrase above into concrete actions
    throw new PendingException()
  }
  Then("""^The action gets executed once$""") { () =>
    //// Write code here that turns the phrase above into concrete actions
    throw new PendingException()
  }
}
