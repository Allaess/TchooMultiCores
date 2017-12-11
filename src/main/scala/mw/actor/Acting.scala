package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Try}

trait Acting {
  acting =>
  implicit val exec: ExecutionContext
  val creator: Actor[Acting]
  implicit lazy val self: Actor[acting.type] = new Actor[acting.type] {
    private var inbox = Promise[Inbox]
    execute(inbox.future)
    def ![U](msg: acting.type => U): Unit = enqueue(inbox, Inbox(msg))
    def failure = inbox.future.value match {
      case Some(Failure(error)) => Some(error)
      case _ => None
    }
    def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = self ! { acting =>
      if (acting.onFailure(caller, error)) {
        creator.fail(error)(self)
        throw FailedException(error)
      }
    }
    private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
      if (promise.trySuccess(elem)) inbox = elem.tail
      else for {
        trie <- promise.future.value
        next <- trie
      } {
        enqueue(next.tail, elem)
      }
    }
    private def enqueue(promise: Promise[Inbox], error: Throwable): Unit = {
      if (promise.tryFailure(error)) inbox = promise
      else for {
        trie <- promise.future.value
        next <- trie
      } {
        enqueue(next.tail, error)
      }
    }
    private def execute(future: Future[Inbox]): Unit = for (elem <- future) {
      Try(elem.head(acting)) match {
        case Failure(FailedException(error)) => enqueue(inbox, error)
        case _ => execute(elem.tail.future)
      }
    }
    private case class Inbox(head: acting.type => Any) {
      val tail = Promise[Inbox]
    }
    private case class FailedException(error: Throwable) extends Exception(error)
  }
  def onFailure(actor: Actor[Acting], error: Throwable): Boolean = true
}
