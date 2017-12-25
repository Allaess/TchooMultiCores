package steps

import cucumber.api.scala.{EN, ScalaDsl}
import mw.actor._
import mw.react.streams.{Processor, Publisher, Subscriber}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext

class Streams extends ScalaDsl with EN with Matchers {
  val exec = scala.concurrent.ExecutionContext.Implicits.global
  val root = Root.actor(exec)
  var app: Actor[MyApp] = _
  var stringResult = ""
  class MyApp(val creator: Actor[Acting])(implicit val exec: ExecutionContext)
    extends Application {
    val source = Publisher[String]
    val subscriber = Subscriber[String, Unit] { text =>
      stringResult = text
    }
    val processor = Processor[String, String] {
      case name => s"Hello $name"
    }
    def start(): Unit = app = self
    def source2subscriber(): Unit = source.subscribe(subscriber)
    def publish(name: String): Unit = source.next(name)
    def source2processor(): Unit = source.subscribe(processor)
    def processor2subscriber(): Unit = processor.subscribe(subscriber)
  }
  object MyApp extends Factory[MyApp] {
    def apply(caller: Actor[Acting], exec: ExecutionContext) = Actor {
      new MyApp(caller)(exec)
    }
  }
  Given("""^I started an Application$""") { () =>
    root ! (_.start(MyApp))
  }
  Given("""^a Subscriber subscribed to a SourcePublisher$""") { () =>
    synchronized(wait(100))
    app ! (_.source2subscriber())
  }
  When("""^the SourcePublisher publishes some data$""") { () =>
    app ! (_.publish("Marc"))
  }
  Then("""^the Subscriber receives the data$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Marc"
  }
  Given("""^a Processor subscribed to a SourcePublisher$""") { () =>
    app ! (_.source2processor())
  }
  Given("""^a Subscriber subscribed to the Processor$""") { () =>
    app ! (_.processor2subscriber())
  }
  Then("""^the Subscriber receives the processed data$""") { () =>
    synchronized(wait(100))
    stringResult shouldBe "Hello Marc"
  }
}
