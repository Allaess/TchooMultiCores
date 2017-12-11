package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor.{Acting, Actor}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ActorsSteps extends ScalaDsl with EN {
  outer =>
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  var firstActor = Actor(new FirstActing()(exec))
  var stringResult = ""
  var futureResult = Future("")(exec)
  var actorResult = Actor.empty[SecondActing]
  class FirstActing(implicit val exec: ExecutionContext) extends Acting {
    val creator = self
    var second = Option.empty[Actor[SecondActing]]
    var third = Option.empty[Actor[SecondActing]]
    def tell(something: String): Unit = stringResult = something
    def askActor(name: String): Actor[SecondActing] = Actor(SecondActing(name))
    def askActing(name: String): SecondActing = SecondActing(name)
    def askFuture(something: String): Future[String] = Future(something)
    def askSomething(something: String): String = something
    def askActorThrows(message: String): Actor[SecondActing] = throw new Exception(message)
    def askFailedActor(name: String, message: String): Actor[SecondActing] = {
      val result = Actor(SecondActing(name))
      result.fail(new Exception(message))
      result
    }
    def askActingThrows(message: String): SecondActing = throw new Exception(message)
    def askFutureThrows(message: String): Future[String] = throw new Exception(message)
    def askFailedFuture(message: String): Future[String] = Future(throw new Exception(message))
    def askSomethingThrows(message: String): String = throw new Exception(message)
    def instantiateSecond(name: String): Unit = second = Some(Actor(SecondActing(name)))
    def instantiateThird(name: String): Unit = third = Some(Actor(SecondActing(name)))
    def doMap(prefix: String): Actor[SecondActing] = {
      val second = this.second.get
      second.map { acting =>
        SecondActing(s"$prefix ${acting.name}")
      }
    }
    def doFlatMap: Actor[SecondActing] = {
      val option = for {
        a <- second
        b <- third
      } yield {
        for {
          s <- a
          t <- b
        } yield {
          SecondActing(s"${t.name} ${s.name}")
        }
      }
      option.get
    }
    def doFilter(name: String): Actor[SecondActing] = {
      val option = for (a <- second) yield {
        a.withFilter(_.name == name)
      }
      option.get
    }
  }
  class SecondActing(val name: String, val creator: Actor[Acting])
                    (implicit val exec: ExecutionContext) extends Acting
  object SecondActing {
    def apply(name: String)(implicit caller: Actor[Acting], exec: ExecutionContext): SecondActing =
      new SecondActing(name, caller)
  }
  Given("""^I instantiate an Actor$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    firstActor = Actor(new FirstActing)
    stringResult = ""
    futureResult = Future("")
    actorResult = Actor.empty[SecondActing]
  }
  When("""^I tell it something$""") { () =>
    firstActor ! (_.tell("String"))
  }
  Then("""^it executes the corresponding method$""") { () =>
    synchronized(wait(100))
    assert(stringResult == "String")
  }
  Then("""^the receiving Actor is not failed$""") { () =>
    synchronized(wait(100))
    assert(!firstActor.isFailed)
  }
  Then("""^the receiving Actor is failed$""") { () =>
    synchronized(wait(100))
    assert(firstActor.isFailed)
  }
  When("""^I ask it something that returns a Future$""") { () =>
    val actor = firstActor
    futureResult = actor ? (_.askFuture("Future"))
  }
  Then("""^I get a Future that will complete to the same value$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    var result = ""
    futureResult.onComplete {
      case Success(text) => result = text
      case _ =>
    }
    synchronized(wait(100))
    assert(result == "Future")
  }
  When("""^I ask it something that returns an Actor$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    val actor = firstActor
    val actorResult = actor ? (_.askActor("Actor"))
    futureResult = actorResult ? (_.name)
  }
  Then("""^I get an Actor with the same behavior$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    futureResult.onComplete {
      case Success(text) => stringResult = text
      case _ =>
    }
    synchronized(wait(100))
    assert(stringResult == "Actor")
  }
  When("""^I ask it something that returns an Acting$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    val actor = firstActor
    val actorResult = actor ? (_.askActing("Actor"))
    futureResult = actorResult ? (_.name)
  }
  When("""^I ask it something that returns anything else$""") { () =>
    val actor = firstActor
    futureResult = actor ? (_.askSomething("Future"))
  }
  When("""^I ask it something that should return an Actor but throws$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    val actor = firstActor
    actorResult = actor ? (_.askActorThrows("Exception"))
  }
  Then("""^I get a failed Actor with the same failure$""") { () =>
    synchronized(wait(100))
    assert(actorResult.failure.get.getMessage == "Exception")
  }
  When("""^I ask it something that returns a failed Actor$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    val actor = firstActor
    actorResult = actor ? (_.askFailedActor("Actor", "Exception"))
  }
  When("""^I ask it something that should return an Acting but throws$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    val actor = firstActor
    actorResult = actor ? (_.askActingThrows("Exception"))
  }
  When("""^I ask it something that should return a Future but throws$""") { () =>
    val actor = firstActor
    futureResult = actor ? (_.askFutureThrows("Exception"))
  }
  Then("""^I get a failed Future with the same failure$""") { () =>
    implicit val exec: ExecutionContext = outer.exec
    futureResult.onComplete {
      case Failure(error) => stringResult = error.getMessage
      case _ =>
    }
    synchronized(wait(100))
    assert(stringResult == "Exception")
  }
  When("""^I ask it something that returns a failed Future$""") { () =>
    val actor = firstActor
    futureResult = actor ? (_.askFailedFuture("Exception"))
  }
  When("""^I ask it something that should return something else but throws$""") { () =>
    val actor = firstActor
    futureResult = actor ? (_.askSomethingThrows("Exception"))
  }
  Given("""^I let it instantiate a second one$""") { () =>
    firstActor ! (_.instantiateSecond("Marc"))
  }
  When("""^I let the first Actor map the second one$""") { () =>
    val actor = firstActor
    implicit val exec: ExecutionContext = outer.exec
    actorResult = actor ? (_.doMap("Hello"))
  }
  Then("""^I get a mapped Actor$""") { () =>
    actorResult ! { acting =>
      stringResult = acting.name
    }
    synchronized(wait(100))
    assert(stringResult == "Hello Marc")
  }
  Given("""^I let it instantiate a third one$""") { () =>
    firstActor ! (_.instantiateThird("Hello"))
  }
  When("""^I let the first Actor flatMap over the two others$""") { () =>
    val actor = firstActor
    implicit val exec: ExecutionContext = outer.exec
    actorResult = actor ? (_.doFlatMap)
  }
  Then("""^I get a flatMapped Actor$""") { () =>
    actorResult ! { acting =>
      stringResult = acting.name
    }
    synchronized(wait(100))
    assert(stringResult == "Hello Marc")
  }
  When("""^I let the first Actor filter over the second one with a matching filter$""") { () =>
    val actor = firstActor
    implicit val exec: ExecutionContext = outer.exec
    actorResult = actor ? (_.doFilter("Marc"))
  }
  Then("""^I get the Actor$""") { () =>
    actorResult ! { acting =>
      stringResult = acting.name
    }
    synchronized(wait(100))
    assert(stringResult == "Marc")
  }
  When("""^I let the first Actor filter over the second one with a non-matching filter$""") { () =>
    val actor = firstActor
    implicit val exec: ExecutionContext = outer.exec
    actorResult = actor ? (_.doFilter("Claire"))
  }
  Then("""^I get an empty Actor$""") { () =>
    actorResult ! { acting =>
      stringResult = acting.name
    }
    synchronized(wait(100))
    assert(stringResult == "")
  }
}
