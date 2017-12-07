package mw.ecos

case class Request(cmd: String, oid: Int, arguments: List[Argument])
