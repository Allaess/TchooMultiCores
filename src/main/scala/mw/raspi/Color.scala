package mw.raspi

case class Color(red: Int, green: Int, blue: Int)
object Color {
  val dark = Color(0, 0, 0)
  val red = Color(255, 0, 0)
  val green = Color(0, 255, 0)
  val blue = Color(0, 0, 255)
  val yellow = Color(127, 255, 0)
  val white = Color(255, 255, 255)
}
