package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor.{Acting, Actor}

import scala.concurrent.{ExecutionContext, Future}

class ActorsSteps extends ScalaDsl with EN {
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  lazy val app = Actor(new MyApp(exec))
  var tellArg: String = _
  var futureResult: Future[String] = _
  lazy val actorResult = app.?(_.self)(app.Ask.actor(app, exec))
  lazy val anotherActorResult = app.?(_.self)(app.Ask.actor(app, exec))
  class MyApp(val exec: ExecutionContext) extends Acting {
    def creator = self
    def tell(arg: String): Unit = tellArg = arg
    def greet(name: String): String = s"Hello $name"
    def askGreet(name: String): Future[String] = self ? (_.greet(name))
  }
  Given("""^I instantiate an actor$""") { () =>
    app
    tellArg = null
    futureResult = null
  }
  When("""^I tell it something$""") { () =>
    app ! (_.tell("Something"))
  }
  Then("""^the corresponding method is executed$""") { () =>
    synchronized {
      wait(100)
    }
    assert(tellArg == "Something")
  }
  When("""^I ask it something that returns a Future$""") { () =>
    futureResult = app ? (_.askGreet("Marc"))
  }
  Then("""^the returned future will contain the answer$""") { () =>
    synchronized {
      wait(100)
    }
    assert(futureResult.value.get.get == "Hello Marc")
  }
  When("""^I ask it something that returns an Actor$""") { () =>
    implicit val caller: Actor[MyApp] = app
    implicit val exec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    actorResult
  }
  Then("""^the returned actor will contain the answer$""") { () =>
    val t1 = app ? (_.toString)
    val t2 = actorResult ? (_.toString)
    synchronized {
      wait(100)
    }
    assert(t1.value.get.get == t2.value.get.get)
  }
  When("""^I ask it something that returns anything else$""") { () =>
    futureResult = app ? (_.greet("Marc"))
  }
  Given("""^I make it fail$""") { () =>
    app ! { _ =>
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
    assert(actorResult.isFailed)
  }
  When("""^I ask it again something that returns an Actor$""") { () =>
    anotherActorResult
  }
  Then("""^it returns another failed Actor$""") { () =>
    assert(anotherActorResult.isFailed)
  }
}
