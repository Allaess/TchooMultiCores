package mw.react.streams

trait Publisher[+T] {
  def subscribe(subscriber: Subscriber[T]): Unit
}
