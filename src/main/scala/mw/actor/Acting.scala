package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Acting {
  def creator: Actor[Acting]
  implicit def exec: ExecutionContext
  implicit val self: Actor[this.type] = new Actor[this.type] {
    private var inbox = Promise[Inbox]
    execute(inbox.future)
    def creator = Acting.this.creator
    def ![U](msg: Acting.this.type => U): Unit = enqueue(inbox, Inbox(msg))
    override def failed(error: Throwable)(implicit caller: Actor[Acting]): Unit =
      creator.failed(error)(self)
    private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
      if (promise.trySuccess(elem)) inbox = elem.tail
      else enqueue(promise.future.value.get.get.tail, elem)
    }
    private def execute(future: Future[Inbox]): Unit = for (elem <- future) {
      Try(elem.head(Acting.this)) match {
        case Success(_) => execute(elem.tail.future)
        case Failure(e) => creator.failed(e)
      }
    }
    private case class Inbox(head: Acting.this.type => Any) {
      val tail = Promise[Inbox]
    }
  }
}
