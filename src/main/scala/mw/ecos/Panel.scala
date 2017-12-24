package mw.ecos

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket

import mw.actor.{Acting, Actor, Application}
import mw.react.streams.SourcePublisher

import scala.concurrent.ExecutionContext
import scala.io.Source

class Panel(name: String, port: Int = 15471, val creator: Actor[Acting])
           (implicit val exec: ExecutionContext) extends EcosParser with Application {
  val socket = new Socket(name, port)
  val source = Source.fromInputStream(socket.getInputStream, "UTF-8").getLines
  val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8"))
  val stringPublisher = SourcePublisher.inner[String]
  private object Runner extends Runnable {
    def run(): Unit = {
      for (line <- source) {
        stringPublisher ! (_.publish(line))
      }
    }
  }
  def start(): Unit = {
    new Thread(Runner).start()
  }
}
