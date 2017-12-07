package mw.raspi

sealed trait State
case object Off extends State
case object Left extends State
case object Right extends State
