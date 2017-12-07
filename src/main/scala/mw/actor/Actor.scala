package mw.actor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

trait Actor[+A <: Acting] {
  self =>
  implicit def exec: ExecutionContext = creator.exec
  protected def creator: Actor[Acting]
  def ![R](msg: A => R): Unit
  def ?[R, S](msg: A => R)(implicit ask: Ask[R, S]): S = ask(msg)
  trait Ask[-R, +S] extends Ask2[R, S]
  trait Ask2[-R, +S] extends ((A => R) => S)
  object Ask {
    implicit def future[F]: Ask[Future[F], Future[F]] = { msg =>
      Ask2.default(msg).flatten
    }
    implicit def actor[B <: Acting](implicit creator: Actor[Acting]): Ask[Actor[B], Actor[B]] = { msg =>
      FutureActor(Ask2.default(msg))
    }
    implicit def acting[B <: Acting](implicit creator: Actor[Acting]): Ask[B, Actor[B]] = { msg =>
      actor(creator)(msg andThen Actor.apply)
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
  def map[B <: Acting](f: A => B)(implicit creator: Actor[Acting]): Actor[B] = self ? f
  def flatMap[B <: Acting](f: A => Actor[B])(implicit creator: Actor[Acting]): Actor[B] = self ? f
  def withFilter(p: A => Boolean)(implicit creator: Actor[Acting]): Actor[A] = self ? { acting =>
    if (p(acting)) Actor(acting)
    else Actor.empty
  }
  def flatten[B <: Acting](implicit id: A => Actor[B], creator: Actor[Acting]): Actor[B] = flatMap(id)
  def collect[B <: Acting](pf: PartialFunction[A, B])(implicit creator: Actor[Acting]): Actor[B] =
    withFilter(pf.isDefinedAt).map(pf)
  def foreach[R](f: A => R): Unit = self ! f
}
object Actor {
  val empty: Actor[Nothing] = new Actor[Nothing] {
    val creator = this
    def ![R](msg: Nothing => R): Unit = {}
  }
  def apply[A <: Acting](acting: A): Actor[A] = acting.self
}
