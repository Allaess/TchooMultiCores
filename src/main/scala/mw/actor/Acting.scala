package mw.actor

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

trait Acting {
  acting =>
  implicit def exec: ExecutionContext = self.exec
  protected def creator: Actor[Acting]
  implicit val self: Actor[acting.type] = new Actor[acting.type] {
    private var ok = true
    private var inbox = Promise[Inbox]
    protected def creator = acting.creator
    execute(inbox)
    def ![R](msg: acting.type => R): Unit = if (ok) enqueue(inbox, Inbox(msg))
    private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
      if (promise.trySuccess(elem)) inbox = elem.tail
      else promise.future.value match {
        case Some(Success(next)) => enqueue(next.tail, elem)
        case _ => throw new Exception("Bug")
      }
    }
    private def execute(promise: Promise[Inbox]): Unit = if (ok) {
      promise.future.onComplete {
        case Success(next) => Try(next.head(acting)) match {
          case Success(_) => execute(next.tail)
          case Failure(error) => failed(error)
        }
        case Failure(error) => failed(error)
      }
    }
    private def failed(error: Throwable): Unit = {
      ok = false
      acting.failed(error)
    }
    case class Inbox(head: acting.type => Any) {
      val tail = Promise[Inbox]
    }
  }
  def failed(actor: Actor[Acting], error: Throwable): Unit = failed(error)
  protected def failed(error: Throwable): Unit = creator ! (_.failed(self, error))
  object InnerActor {
    def apply[A <: Acting](acting: A)(implicit caller: Actor[Acting]): Actor[A] = new Actor[A] {
      protected val creator = caller
      def ![R](msg: A => R): Unit = self ! { _ => msg(acting) }
    }
  }
}
