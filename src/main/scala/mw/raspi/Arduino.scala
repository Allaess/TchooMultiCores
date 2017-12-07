package mw.raspi

import java.nio.charset.Charset

import com.pi4j.io.serial.{DataBits, _}

object Arduino {
  private var blocs = Map.empty[Int, Bloc]
  private var switches = Map.empty[Int, Switch]
  private val serial = SerialFactory.createInstance
  private val config = new SerialConfig
  config.device(SerialPort.getDefaultPort)
    .baud(Baud._9600)
    .dataBits(DataBits._8)
    .parity(Parity.NONE)
    .stopBits(StopBits._1)
    .flowControl(FlowControl.NONE)
  serial.open(config)
  private def writeln(text: String): Unit = {
    serial.writeln(Charset.forName("UTF-8"), text)
    serial.flush()
  }
  class Bloc(val num: Int) {
    def color_=(color: Color): Unit = {
      val Color(red, green, blue) = color
      writeln(s"b$num,$red,$green,$blue")
    }
    def off(): Unit = color_=(Color.dark)
  }
  class Switch(val num: Int, private var _state: State = Off, private var _color: Color = Color.dark) {
    def color_=(color: Color): Unit = {
      _color = color
      update()
    }
    def state_=(state: State): Unit = {
      _state = state
      update()
    }
    def off(): Unit = {
      _state = Off
      update()
    }
    private def update(): Unit = {
      val Color(red, green, blue) = _color
      _state match {
        case Off =>
          writeln(s"s$num,0,0,0")
        case Left =>
          writeln(s"s$num,0,0,0")
          writeln(s"l$num,$red,$green,$blue")
        case Right =>
          writeln(s"s$num,0,0,0")
          writeln(s"r$num,$red,$green,$blue")
      }
    }
  }
}
