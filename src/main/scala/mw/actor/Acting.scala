package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Acting {
  def creator: Actor[Acting]
  implicit def exec: ExecutionContext
  def failed(error: Throwable): Unit = self.failed(error)
  def onFailure(actor: Actor[Acting], error: Throwable): Boolean = true
  implicit val self: Actor[this.type] = new Actor[this.type] {
    private var inbox = Promise[Inbox]
    execute(inbox.future)
    def creator = Acting.this.creator
    def ![U](msg: Acting.this.type => U): Unit = error match {
      case None => enqueue(inbox, Inbox(msg))
      case Some(_) =>
    }
    override def failed(error: Throwable)(implicit caller: Actor[Acting]): Unit = {
      if (Acting.this.onFailure(caller, error)) super.failed(error)
    }
    override protected def onFailure(error: Throwable): Unit = {
      if (Acting.this.onFailure(self, error)) super.onFailure(error)
    }
    private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
      if (promise.trySuccess(elem)) inbox = elem.tail
      else enqueue(promise.future.value.get.get.tail, elem)
    }
    private def execute(future: Future[Inbox]): Unit = for (elem <- future) {
      Try(elem.head(Acting.this)) match {
        case Success(_) => execute(elem.tail.future)
        case Failure(e) => onFailure(e)
      }
    }
    private case class Inbox(head: Acting.this.type => Any) {
      val tail = Promise[Inbox]
    }
  }
}
