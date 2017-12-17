package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Actor[+A <: Acting] {
  self =>
  def ![U](msg: A => U): Unit
  def ?[R, S](msg: A => R)(implicit ask: Ask[R, S]): S = ask(msg)
  trait Ask[-R, +S] extends Ask2[R, S]
  trait Ask2[-R, +S] extends ((A => R) => S)
  object Ask {
    implicit def future[F]: Ask[Future[F], Future[F]] = { msg =>
      Ask2.default(msg).flatten
    }
    implicit def actor[B <: Acting](implicit caller: Actor[Acting],
                                    exec: ExecutionContext): Ask[Actor[B], Actor[B]] = { msg =>
      Actor.now(Ask2.default(msg))
    }
    implicit def acting[B <: Acting](implicit caller: Actor[Acting],
                                     exec: ExecutionContext): Ask[B, Actor[B]] = { msg =>
      actor(caller, exec)(msg andThen Actor.apply)
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
  def map[B <: Acting](f: A => B)(implicit caller: Actor[Acting],
                                  exec: ExecutionContext): Actor[B] = self ? f
  def flatMap[B <: Acting](f: A => Actor[B])(implicit caller: Actor[Acting],
                                             exec: ExecutionContext): Actor[B] = self ? f
  def withFilter(p: A => Boolean)(implicit caller: Actor[Acting],
                                  exec: ExecutionContext): Actor[A] = self ? {
    case acting if p(acting) => Actor(acting)
    case _ => Actor.empty
  }
  def flatten[B <: Acting](implicit id: A => Actor[B],
                           caller: Actor[Acting],
                           exec: ExecutionContext): Actor[B] = flatMap(id)
  def collect[B <: Acting](pf: PartialFunction[A, B])(implicit caller: Actor[Acting],
                                                      exec: ExecutionContext): Actor[B] =
    withFilter(pf.isDefinedAt).map(pf)
}
object Actor {
  lazy val root = Actor(new Root()(scala.concurrent.ExecutionContext.Implicits.global))
  def empty[A <: Acting]: Actor[A] = new Actor[Nothing] {
    def ![U](msg: Nothing => U): Unit = {}
  }
  def apply[A <: Acting](acting: A): Actor[A] = acting.self
  def inner[A <: Acting](acting: A)(implicit caller: Actor[Acting]): Actor[A] = new Actor[A] {
    def ![U](msg: A => U): Unit = caller ! { _ =>
      msg(acting)
    }
  }
  def now[A <: Acting](futureActor: Future[Actor[A]])
                      (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[A] =
    new Actor[A] {
      private var inbox = Promise[Inbox]
      execute(inbox.future)
      def ![U](msg: A => U): Unit = enqueue(inbox, Inbox(msg))
      private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
        if (promise.trySuccess(elem)) inbox = elem.tail
        else for {
          trie <- promise.future.value
          next <- trie
        } {
          enqueue(next.tail, elem)
        }
      }
      private def execute(futureElem: Future[Inbox]): Unit = futureActor.onComplete {
        case Success(actor) => for (elem <- futureElem) {
          actor ! elem.head
          execute(elem.tail.future)
        }
        case Failure(error) => caller ! (_.fail(error))
      }
      private case class Inbox(head: A => Any) {
        val tail = Promise[Inbox]
      }
    }
}
