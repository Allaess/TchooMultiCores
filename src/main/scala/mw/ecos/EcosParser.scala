package mw.ecos

import scala.util.parsing.combinator.RegexParsers

trait EcosParser extends RegexParsers {
  def line: Parser[Line] = eventLine | replyLine | listEntry | endLine
  def eventLine: Parser[EventLine] = "<" ~> "EVENT" ~> int <~ ">" ^^ { oid =>
    EventLine(oid)
  }
  def replyLine: Parser[ReplyLine] = "<" ~> "REPLY" ~> (request | "?") <~ ">" ^^ {
    case request: Request => ReplyLine(Some(request))
    case _: String => ReplyLine(None)
  }
  def endLine: Parser[EndLine] = "<" ~> "END" ~> int ~ ("(" ~> errMsg <~ ")") <~ ">" ^^ {
    case errNo ~ errMsg => EndLine(errNo, errMsg)
  }
  def request: Parser[Request] = token ~ ("(" ~> int ~ ("," ~> argument).* <~ ")") ^^ {
    case cmd ~ (oid ~ args) => Request(cmd, oid, args)
  }
  def listEntry: Parser[ListEntry] = int ~ argument.* ^^ {
    case oid ~ args => ListEntry(oid, args)
  }
  def argument: Parser[Argument] = token ~ parameter.? ^^ {
    case option ~ Some(params) => Argument(option, params)
    case option ~ None => Argument(option, Nil)
  }
  def parameter: Parser[List[String]] = "[" ~> (value ~ ("," ~> value).*).? <~ "]" ^^ {
    case Some(head ~ Nil) => head :: Nil
    case Some(head ~ tail) => head :: tail
    case None => Nil
  }
  def errMsg: Parser[String] = """[^\(\)]+""".r
  def value: Parser[String] = string | keyword
  def string: Parser[String] =
    """"[^"]*"""".r ^^ {
      text => text.drop(1).dropRight(1)
    }
  def keyword: Parser[String] = """[^,"\[\]]+""".r
  def token: Parser[String] = """[a-zA-Z0-9_\-]+""".r
  def int: Parser[Int] =
    """[0-9]+""".r ^^ {
      text => text.toInt
    }
  def parseLine(text: String): Either[Line, String] = parseAll(line, text) match {
    case Success(obj, _) => Left(obj)
    case NoSuccess(msg, _) => Right(msg)
  }
}
