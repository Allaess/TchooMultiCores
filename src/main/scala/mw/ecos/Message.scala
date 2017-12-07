package mw.ecos

sealed trait Message {
  def end: EndLine
  def oid: Option[Int]
  def entries: List[ListEntry]
  def errorCode: Int = end.errNo
  def errorMessage: String = end.errMsg
}
case class Event(start: EventLine, entries: List[ListEntry], end: EndLine) extends Message {
  def oid = Some(start.oid)
}
case class Reply(start: ReplyLine, entries: List[ListEntry], end: EndLine) extends Message {
  def oid = start.request.map(_.oid)
}
