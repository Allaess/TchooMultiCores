package mw.react.streams

trait Source[-T] {
  def next(t: T): Unit
  def complete(): Unit
  def error(throwable: Throwable): Unit
}
