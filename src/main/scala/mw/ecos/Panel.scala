package mw.ecos

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket
import java.nio.charset.Charset

import mw.actor._
import mw.react.streams.Processor

import scala.io.{Codec, Source}

class Panel(name: String, port: Int, val creator: Actor[Acting])
  extends EcosParser with Application {
  private val socket = new Socket(name, port)
  private val source = Source.fromInputStream(socket.getInputStream)(Codec.UTF8).getLines
  private val writer =
    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, Charset.forName("UTF-8")))
  private val stream = Processor[String, Either[Line, String]] {
    case text => parseLine(text)
  }
  val messages = stream.collect {
    case Left(line) => line
  }.scan((Option.empty[StartLine], List.empty[ListEntry], Option.empty[EndLine])) {
    case (_, line: StartLine) => (Some(line), Nil, None)
    case ((Some(start), entries, None), line: ListEntry) => (Some(start), line :: entries, None)
    case ((Some(start), entries, None), line: EndLine) => (Some(start), entries, Some(line))
    case _ => (None, Nil, None)
  }.collect {
    case (Some(start: EventLine), entries, Some(end)) => Event(start, entries.reverse, end)
    case (Some(start: ReplyLine), entries, Some(end)) => Reply(start, entries.reverse, end)
  }
  stream.collect {
    case Right(error) => System.err.println(error)
  }
  def this(name: String)(implicit caller: Actor[Acting]) = this(name, 15471, caller)
  def start(): Unit = {
    write("get(1,info)")
    for (message <- messages) println(message)
    for (text <- source) stream.next(text)
  }
  def write(command: String): Unit = {
    writer.write(command)
    writer.newLine()
    writer.flush()
  }
  def close(): Unit = socket.close()
}
object Panel extends Factory[Panel] {
  type Args = String
  def apply(args: Args)(implicit creator: Actor[Acting]) = Actor(new Panel(args))
}
