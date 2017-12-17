package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Acting {
  acting =>
  implicit def exec: ExecutionContext
  implicit val self: Actor[acting.type] = new Actor[acting.type] {
    private var inbox = Promise[Inbox]
    execute(inbox.future)
    def ![U](msg: acting.type => U): Unit = {
      enqueue(inbox, Inbox(msg))
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
    private def execute(future: Future[Inbox]): Unit = for (elem <- future) {
      Try(elem.head(acting)) match {
        case Success(_) => execute(elem.tail.future)
        case Failure(e) => fail(e)
      }
    }
    override def toString = s"Actor(${acting.getClass.getName}(${acting.hashCode}))"
    case class Inbox(head: acting.type => Any) {
      val tail = Promise[Inbox]
    }
  }
  def creator: Actor[Acting]
  def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = {
    creator ! (_.fail(error)(self))
  }
}
