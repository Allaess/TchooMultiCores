package mw.ecos

case class Request(cmd: String, oid: Int, args: List[Argument])
