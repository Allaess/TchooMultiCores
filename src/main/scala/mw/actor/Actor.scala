package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Actor[+A <: Acting] {
  self =>
  def ![U](msg: A => U): Unit
  def ?[R, S](msg: A => R)(implicit ask: Ask[R, S]): S = ask(msg)
  def failure: Option[Throwable]
  def isFailed = failure.nonEmpty
  def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit
  trait Ask[-R, +S] extends Ask2[R, S]
  trait Ask2[-R, +S] extends ((A => R) => S)
  object Ask {
    implicit def future[F]: Ask[Future[F], Future[F]] = { msg =>
      Ask2.default(msg).flatten
    }
    implicit def actor[B <: Acting](implicit exec: ExecutionContext): Ask[Actor[B], Actor[B]] = { msg =>
      Actor(Ask2.default(msg))
    }
    implicit def acting[B <: Acting](implicit exec: ExecutionContext): Ask[B, Actor[B]] = { msg =>
      Actor(Ask2.default(msg).map(Actor(_)))
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
  def map[B <: Acting](f: A => B)(implicit exec: ExecutionContext): Actor[B] = self ? f
  def flatMap[B <: Acting](f: A => Actor[B])(implicit exec: ExecutionContext): Actor[B] = self ? f
  def withFilter(p: A => Boolean)(implicit exec: ExecutionContext): Actor[A] = self ? {
    case acting if p(acting) => acting.self
    case _ => Actor.empty
  }
  def flatten[B <: Acting](implicit id: A => Actor[B], exec: ExecutionContext): Actor[B] = flatMap(id)
  def collect[B <: Acting](pf: PartialFunction[A, B])(implicit exec: ExecutionContext): Actor[B] =
    self ? {
      case acting if pf.isDefinedAt(acting) => Actor(pf(acting))
      case _ => Actor.empty
    }
  def foreach[U](f: A => U): Unit = self ! f
}
object Actor {
  def empty[A <: Acting]: Actor[A] = new Actor[Nothing] {
    private val error = Promise[Throwable]
    def ![U](msg: Nothing => U): Unit = {}
    def failure = error.future.value match {
      case Some(Success(err)) => Some(err)
      case Some(Failure(err)) => Some(err)
      case None => None
    }
    def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = this.error.trySuccess(error)
  }
  def inner[A <: Acting](acting: A)(implicit creator: Actor[Acting]): Actor[A] = new Actor[A] {
    def ![U](msg: A => U): Unit = creator ! { _ =>
      msg(acting)
    }
    def failure = creator.failure
    def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = creator ! { _ =>
      if (acting.onFailure(caller, error)) {
        creator.fail(error)(this)
      }
    }
  }
  def apply[A <: Acting](acting: A): Actor[A] = acting.self
  def apply[A <: Acting](futureActor: Future[Actor[A]])
                        (implicit exec: ExecutionContext): Actor[A] = new Actor[A] {
    private var inbox = Promise[Inbox]
    execute(inbox.future)
    def ![U](msg: A => U): Unit = enqueue(inbox, Inbox(msg))
    def failure = futureActor.value match {
      case Some(Success(actor)) => actor.failure
      case Some(Failure(error)) => Some(error)
      case None => None
    }
    def fail(error: Throwable)(implicit caller: Actor[Acting]): Unit = {
      this ! (_.self.fail(error))
      enqueue(inbox, error)
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
    private def execute(futureElem: Future[Inbox]): Unit = futureActor.onComplete {
      case Success(actor) => execute(actor, futureElem)
      case Failure(error) => enqueue(inbox, error)
    }
    private def execute(actor: Actor[A], futureElem: Future[Inbox]): Unit = actor.failure match {
      case None => for (elem <- futureElem) {
        actor ! elem.head
        execute(actor, elem.tail.future)
      }
      case Some(error) => enqueue(inbox, error)
    }
    private case class Inbox(head: A => Any) {
      val tail = Promise[Inbox]
    }
  }
}
