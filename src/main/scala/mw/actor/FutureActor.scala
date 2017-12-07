package mw.actor

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

final case class FutureActor[A <: Acting](future: Future[Actor[A]])(implicit val creator: Actor[Acting])
  extends Actor[A] {
  self =>
  private var inbox = Promise[Inbox]
  private var ok = true
  execute(inbox)
  def ![R](msg: A => R): Unit = if (ok) enqueue(inbox, new Inbox(msg))
  private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit =
    if (promise.trySuccess(elem)) inbox = elem.tail
    else promise.future.value match {
      case Some(Success(next)) => enqueue(next.tail, elem)
      case _ => throw new Exception("Bug")
    }
  private def execute(promise: Promise[Inbox]): Unit = if (ok) {
    val unit = for {
      actor <- future
      elem <- promise.future
    } yield {
      actor ! elem.head
      execute(elem.tail)
    }
    unit.onComplete {
      case Failure(e) => failed(e)
      case _ =>
    }
  }
  private def failed(error: Throwable): Unit = {
    ok = false
    creator ! (_.failed(self, error))
  }
  class Inbox(val head: A => Any) {
    val tail = Promise[Inbox]
  }
}
