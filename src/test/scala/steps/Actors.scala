package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor
import mw.actor._
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

class Actors extends ScalaDsl with EN with Matchers {
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  lazy val root = actor.Actor(new Root()(exec))
  var created = ""
  var application: Actor[MyApp] = _
  var stringResult = ""
  var actorResult: Actor[MyActing] = _
  class MyApp(val name: String, val creator: Actor[Acting])
             (implicit val exec: ExecutionContext) extends Application {
    created = name
    val actor = Actor(MyActing("Marc"))
    def start(): Unit = application = self
    def fail(): Unit = {
      application = null
      throw new Exception("Dummy")
    }
    def tell(something: String): Unit = actor ! (_.tell(something))
    def askActor(name: String): Unit = for (acting <- actor ? (_.askActor(name))) {
      stringResult = acting.name
    }
    def askActing(name: String): Unit = for (acting <- actor ? (_.askActing(name))) {
      stringResult = acting.name
    }
    def askFuture(name: String): Unit = for (string <- actor ? (_.askFuture(name))) {
      stringResult = string
    }
    def askString(name: String): Unit = for (string <- actor ? (_.askString(name))) {
      stringResult = string
    }
    def mapActor(): Unit = {
      val result = actor.map { acting =>
        MyActing(s"${acting.name} Wautelet")
      }
      for (acting <- result) {
        stringResult = acting.name
      }
    }
    def flatMapActor(): Unit = {
      val result = actor.flatMap { acting =>
        Actor(MyActing(s"${acting.name} Wautelet"))
      }
      for (acting <- result) {
        stringResult = acting.name
      }
    }
    def filterWithSuccess(): Unit = {
      val result = actor.withFilter { acting =>
        acting.name == "Marc"
      }
      for (acting <- result) {
        stringResult = acting.name
      }
    }
    def filterWithFailure(): Unit = {
      val result = actor.withFilter { acting =>
        acting.name == "Beatrice"
      }
      for (acting <- result) {
        stringResult = acting.name
      }
    }
  }
  class MyAppFactory(name: String) extends Factory[MyApp] {
    def apply(caller: Actor[Acting], exec: ExecutionContext) = Actor {
      new MyApp(name, caller)(exec)
    }
  }
  implicit object MyApp extends MyAppFactory("MyApp")
  class MyActing(val name: String, val creator: Actor[Acting])
                (implicit val exec: ExecutionContext) extends Acting {
    def tell(something: String): Unit = stringResult = something
    def askActor(name: String): Actor[MyActing] = Actor(MyActing(name))
    def askActing(name: String): MyActing = MyActing(name)
    def askFuture(name: String): Future[String] = Future(s"Hello $name")
    def askString(name: String): String = s"Hello $name"
  }
  object MyActing {
    def apply(name: String)(implicit caller: Actor[Acting], exec: ExecutionContext) = {
      new MyActing(name, caller)
    }
  }
  Given("""^I instantiated a Root$""") { () =>
    created = ""
    application = null
    stringResult = ""
    root
  }
  When("""^I tell Root to start an Application$""") { () =>
    root ! (_.start[MyApp])
  }
  Then("""^the Application is instantiated$""") { () =>
    synchronized(wait(100))
    created shouldBe "MyApp"
  }
  Then("""^the Application is told to start$""") { () =>
    synchronized(wait(100))
    application shouldBe a[Actor[_]]
  }
  When("""^the Application tells something to an Actor$""") { () =>
    synchronized(wait(100))
    application ! (_.tell("something"))
  }
  Then("""^the Actor executes the corresponding method$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "something"
  }
  When("""^the Application asks another Actor from an Actor$""") { () =>
    synchronized(wait(100))
    application ! (_.askActor("Claire"))
  }
  Then("""^the Actor returns the other Actor$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Claire"
  }
  Given("""^I told Root to start an Application$""") { () =>
    root ! (_.start[MyApp])
  }
  When("""^the Application fails$""") { () =>
    synchronized(wait(100))
    application ! (_.fail())
  }
  Then("""^Root restarts the Application$""") { () =>
    synchronized(wait(100))
    application shouldBe a[Actor[_]]
  }
  When("""^the Application asks an Acting from an Actor$""") { () =>
    synchronized(wait(100))
    application ! (_.askActing("Beatrice"))
  }
  Then("""^the Actor returns the corresponding Actor$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Beatrice"
  }
  When("""^the Application asks a Future from an Actor$""") { () =>
    application ! (_.askFuture("Benoit"))
  }
  Then("""^the Actor returns the Future$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Hello Benoit"
  }
  When("""^the Application asks a String from an Actor$""") { () =>
    application ! (_.askString("Simon"))
  }
  Then("""^the Actor returns a Future$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Hello Simon"
  }
  When("""^the Application maps over an Actor$""") { () =>
    application ! (_.mapActor())
  }
  Then("""^the mapped Actor is returned$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Marc Wautelet"
  }
  When("""^the Application flatMaps over an Actor$""") { () =>
    application ! (_.flatMapActor())
  }
  Then("""^the flatMapped Actor is returned$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Marc Wautelet"
  }
  When("""^the Application filters over an Actor with success$""") { () =>
    synchronized(wait(100))
    application ! (_.filterWithSuccess())
  }
  Then("""^the filtered Actor is returned$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Marc"
  }
}
