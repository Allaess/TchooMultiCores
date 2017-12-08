package mw.react.streams

trait Subscription {
  def cancel(): Unit
}
