package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

trait Actor[+A <: Acting] {
  self =>
  def creator: Actor[Acting]
  def ![U](msg: A => U): Unit
  def ?[R, S](msg: A => R)(implicit ask: Ask[R, S]): S = ask(msg)
  trait Ask[-R, +S] extends Ask2[R, S]
  trait Ask2[-R, +S] extends ((A => R) => S)
  object Ask {
    implicit def actor[B <: Acting](implicit caller: Actor[Acting], exec: ExecutionContext): Ask[Actor[B], Actor[B]] = { msg =>
      Actor(Ask2.default(msg))
    }
    implicit def future[F]: Ask[Future[F], Future[F]] = { msg =>
      Ask2.default(msg).flatten
    }
  }
  object Ask2 {
    implicit def default[F]: Ask[F, Future[F]] = { msg =>
      val promise = Promise[F]
      self ! { acting =>
        promise.complete(Try(msg(acting)))
      }
      promise.future
    }
  }
  def failed(error: Throwable)(implicit caller: Actor[Acting]): Unit = creator.failed(error)
  def map[B <: Acting](f: A => B)
                      (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[B] =
    self ? { acting: A =>
      Actor(f(acting))
    }
  def flatMap[B <: Acting](f: A => Actor[B])
                          (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[B] =
    self ? f
  def withFilter(p: A => Boolean)
                (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[A] =
    self ? {
      case acting if p(acting) => self
      case _ => Actor.empty
    }
  def flatten[B <: Acting](implicit id: A => Actor[B],
                           caller: Actor[Acting],
                           exec: ExecutionContext): Actor[B] =
    flatMap(id)
  def collect[B <: Acting](pf: PartialFunction[A, B])
                          (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[B] =
    self ? {
      case acting if pf.isDefinedAt(acting) => Actor(pf(acting))
      case _ => Actor.empty
    }
  def foreach[U](action: A => U): Unit = self ! action
}
object Actor {
  val empty: Actor[Nothing] = new Actor[Nothing] {
    def creator = this
    def ![U](msg: Nothing => U): Unit = {}
  }
  def apply[A <: Acting](acting: A): Actor[A] = acting.self
  def apply[A <: Acting](futureActor: Future[Actor[A]])
                        (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[A] =
    new Actor[A] {
      private var inbox = Promise[Inbox]
      execute(inbox.future)
      def creator = caller
      def ![U](msg: A => U): Unit = enqueue(inbox, Inbox(msg))
      private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
        if (promise.trySuccess(elem)) inbox = elem.tail
        else enqueue(promise.future.value.get.get.tail, elem)
      }
      private def execute(futureElem: Future[Inbox]): Unit = for {
        actor <- futureActor
        elem <- futureElem
      } {
        actor ! elem.head
        execute(elem.tail.future)
      }
      private case class Inbox(head: A => Any) {
        val tail = Promise[Inbox]
      }
    }
}
