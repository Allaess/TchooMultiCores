package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Actor[+A <: Acting] {
  self =>
  protected var error = Option.empty[Throwable]
  def creator: Actor[Acting]
  def ![U](msg: A => U): Unit
  def ?[R, S](msg: A => R)(implicit ask: Ask[R, S]): S = ask(msg)
  def failed(error: Throwable)(implicit caller: Actor[Acting]): Unit = onFailure(error)
  protected def onFailure(error: Throwable): Unit = {
    this.error = Some(error)
    if (creator != self) creator.failed(error)(self)
  }
  def isFailed = error.nonEmpty
  trait Ask[-R, +S] extends Ask2[R, S]
  trait Ask2[-R, +S] extends ((A => R) => S)
  object Ask {
    implicit def actor[B <: Acting]
    (implicit caller: Actor[Acting], exec: ExecutionContext): Ask[Actor[B], Actor[B]] = { msg =>
      Actor(Ask2.default(msg))
    }
    implicit def future[F]: Ask[Future[F], Future[F]] = { msg =>
      Ask2.default(msg).flatten
    }
  }
  object Ask2 {
    implicit def default[F]: Ask[F, Future[F]] = { msg =>
      error match {
        case None =>
          val promise = Promise[F]
          self ! { acting =>
            promise.tryComplete(Try(msg(acting)))
          }
          promise.future
        case Some(err) =>
          Future.failed(err)
      }
    }
  }
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
  def empty: Actor[Nothing] = new Actor[Nothing] {
    def creator = this
    def ![U](msg: Nothing => U): Unit = {}
    override protected def onFailure(error: Throwable): Unit = {
      this.error = Some(error)
    }
  }
  def apply[A <: Acting](acting: A): Actor[A] = acting.self
  def apply[A <: Acting](futureActor: Future[Actor[A]])
                        (implicit caller: Actor[Acting], exec: ExecutionContext): Actor[A] =
    new Actor[A] {
      self =>
      private var inbox = Promise[Inbox]
      execute(inbox.future)
      def creator = caller
      def ![U](msg: A => U): Unit = error match {
        case None => enqueue(inbox, Inbox(msg))
        case _ =>
      }
      override def isFailed = {
        if (super.isFailed) true
        else futureActor.value match {
          case Some(Success(actor)) => actor.isFailed
          case Some(Failure(_)) => true
          case None => false
        }
      }
      override protected def onFailure(error: Throwable): Unit = {
        self ! (_.self.failed(error)(self)) // enqueue a failure for futureActor
        super.onFailure(error)
      }
      private def enqueue(promise: Promise[Inbox], elem: Inbox): Unit = {
        if (promise.trySuccess(elem)) inbox = elem.tail
        else enqueue(promise.future.value.get.get.tail, elem)
      }
      private def execute(futureElem: Future[Inbox]): Unit =
        futureActor.onComplete {
          case Success(actor) => futureElem.onComplete {
            case Success(elem) => actor ! elem.head
            case Failure(err) => onFailure(err)
          }
          case Failure(err) => onFailure(err)
        }
      private case class Inbox(head: A => Any) {
        val tail = Promise[Inbox]
      }
    }
  def inner[A <: Acting](acting: A)(implicit caller: Actor[Acting]): Actor[A] = new Actor[A] {
    self =>
    def creator = caller
    def ![U](msg: A => U): Unit = error match {
      case None => creator ! { _ => msg(acting) } // delegate execution to creator
      case _ =>
    }
  }
}
