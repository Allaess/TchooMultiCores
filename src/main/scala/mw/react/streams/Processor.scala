package mw.react.streams

trait Processor[-T, +S] extends Subscriber[T] with Publisher[S] {

}
