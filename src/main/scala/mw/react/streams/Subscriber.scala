package mw.react.streams

trait Subscriber[-T] {
  def subscribed(subscription: Subscription): Unit
  def next(t: T): Unit
  def complete(): Unit
  def error(error: Throwable): Unit
}
