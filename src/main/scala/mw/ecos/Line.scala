package mw.ecos

sealed trait Line
sealed trait StartLine extends Line
case class EventLine(oid: Int) extends StartLine
case class ReplyLine(request: Option[Request]) extends StartLine
case class ListEntry(oid: Int, args: List[Argument]) extends Line
case class EndLine(errNum: Int, errMsg: String) extends Line
