package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor.{Acting, Actor}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ActorsSteps extends ScalaDsl with EN {
  outer =>
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  lazy val firstActor = Actor(FirstActing()(exec))
  var told = ""
  var futureString = Future("")(exec)
  var actorResult = Actor.empty[SecondActing]
  class FirstActing(implicit val exec: ExecutionContext) extends Acting {
    private lazy val other = Actor(SecondActing())
    private lazy val askedActor = other ? (_.askActor)
    val creator = self
    def tell(something: String): Unit = told = something
    def tellOther(something: String): Unit = other ! (_.tell(something))
    def createOther(): Unit = other
    def greet(name: String): String = s"Hello $name"
    def askOther(name: String): Future[String] = other ? (_.greet(name))
    def askActor(): Unit = askedActor
    def identifyActorResult(): Future[String] = askedActor ? (_.identify)
    def failedFuture: Future[String] = Future(throw new Exception("Intentional"))
    def throwingFuture: Future[String] = throw new Exception("Intentional")
    def throwingActor: Actor[SecondActing] = throw new Exception("Intentional")
    def throwingString: String = throw new Exception("Intentional")
  }
  object FirstActing {
    def apply()(implicit exec: ExecutionContext) = new FirstActing()
  }
  class SecondActing(val creator: Actor[Acting])(implicit val exec: ExecutionContext) extends Acting {
    def tell(something: String): Unit = told = something
    def greet(name: String): String = s"Hello $name"
    def askActor: Actor[ThirdActing] = Actor(ThirdActing())
  }
  object SecondActing {
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) = new SecondActing(caller)
  }
  class ThirdActing(val creator: Actor[Acting])(implicit val exec: ExecutionContext) extends Acting {
    def identify: String = "Third Actor"
  }
  object ThirdActing {
    def apply()(implicit caller: Actor[Acting], exec: ExecutionContext) = new ThirdActing(caller)
  }
  Given("""^I instantiate an Actor$""") { () =>
    firstActor
    told = ""
    futureString = Future("")(exec)
    actorResult = Actor.empty
  }
  When("""^I tell it something$""") { () =>
    firstActor ! (_.tell("Kiss"))
  }
  Then("""^it executes the told method$""") { () =>
    synchronized {
      wait(100)
    }
    assert(told == "Kiss")
  }
  When("""^the first Actor tells something to the second one$""") { () =>
    firstActor ! (_.tellOther("Love"))
  }
  Then("""^the second Actor executes the told function$""") { () =>
    synchronized {
      wait(100)
    }
    assert(told == "Love")
  }
  Given("""^I let the first Actor instantiate another one$""") { () =>
    firstActor ! (_.createOther())
  }
  When("""^I ask it something that returns neither an Actor neither a Future$""") { () =>
    futureString = firstActor ? (_.greet("Marc"))
  }
  Then("""^I receive a future that will contain the answer$""") { () =>
    var result = ""
    futureString.onComplete {
      case Success(text) => result = text
      case _ =>
    }(exec)
    synchronized {
      wait(100)
    }
    assert(result == "Hello Marc")
  }
  When("""^I ask it something that returns a Future$""") { () =>
    futureString = firstActor ? (_.askOther("Marc"))
  }
  When("""^I tell the first Actor to ask the second Actor for something that returns a third Actor$""") { () =>
    firstActor ! (_.askActor())
  }
  Then("""^the first Actor receives an Actor that will contain the answer$""") { () =>
    var result = Try("")
    val future = firstActor ? (_.identifyActorResult())
    future.onComplete(result = _)(exec)
    synchronized {
      wait(100)
    }
    assert(result == Success("Third Actor"))
  }
  When("""^I ask it something that returns a failed Future$""") { () =>
    futureString = firstActor ? (_.failedFuture)
  }
  Then("""^I receive a Future that will fail with the same Exception$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    var failure = Option.empty[Throwable]
    futureString.onComplete {
      case Failure(error) => failure = Some(error)
      case _ =>
    }
    synchronized {
      wait(100)
    }
    assert(failure.get.getMessage == "Intentional")
  }
  When("""^I ask it something that should return a Future but throws$""") { () =>
    futureString = firstActor ? (_.throwingFuture)
  }
  When("""^I ask it something that should return an Actor but throws$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    actorResult = firstActor ? (_.throwingActor)
  }
  Then("""^I received a failed Actor$""") { () =>
    assert(actorResult.isFailed)
  }
  Then("""^The Actor is failed with the same Exception$""") { () =>
    assert(actorResult.failure.get.getMessage == "Intentional")
  }
  When("""^I ask it something that should return neither a Future neither an Actor but throws$""") { () =>
    futureString = firstActor ? (_.throwingString)
  }
}
