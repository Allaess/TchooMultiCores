package mw.ecos

import mw.actor.{Actor, Root}

object Test extends App {
  val system = Actor(new Root)
  system ! (_.start(Panel)("192.168.1.68"))
}
