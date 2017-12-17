package mw.ecos

import scala.util.parsing.combinator.RegexParsers

trait EcosParser extends RegexParsers {
  private def line: Parser[Line] = startLine | listEntry | endLine
  private def startLine: Parser[StartLine] = eventLine | replyLine
  private def eventLine: Parser[EventLine] = "<" ~> "EVENT" ~> int <~ ">" ^^ { oid =>
    EventLine(oid)
  }
  private def replyLine: Parser[ReplyLine] = "<" ~> "REPLY" ~> (request | "?") <~ ">" ^^ {
    case r: Request => ReplyLine(Some(r))
    case _ => ReplyLine(None)
  }
  private def listEntry: Parser[ListEntry] = int ~ argument.* ^^ {
    case oid ~ args => ListEntry(oid, args)
  }
  private def endLine: Parser[EndLine] = "<" ~> "END" ~> int ~ "(" ~ errMsg <~ ")" <~ ">" ^^ {
    case code ~ _ ~ msg => EndLine(code, msg)
  }
  private def int: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
  private def errMsg: Parser[String] = """[^\(\)]+""".r
  private def request: Parser[Request] = keyword ~ "(" ~ int ~ ("," ~> argument).* <~ ")" ^^ {
    case cmd ~ _ ~ oid ~ args => Request(cmd, oid, args)
  }
  private def argument: Parser[Argument] =
    keyword ~ ("[" ~> (value ~ ("," ~> value).*).? <~ "]").? ^^ {
      case option ~ None => Argument(option, Nil)
      case option ~ Some(None) => Argument(option, Nil)
      case option ~ Some(Some(head ~ tail)) => Argument(option, head :: tail)
    }
  private def keyword: Parser[String] = """[0-9a-zA-Z_\-]+""".r
  private def value: Parser[String] = stringValue | otherValue
  private def stringValue: Parser[String] = """"[^\"]*"""".r ^^ (_.drop(1).dropRight(1))
  private def otherValue: Parser[String] ="""[^\",\[\]]+""".r
  def parseLine(text: String): Either[Line, String] = parseAll(line, text) match {
    case Success(result, _) => Left(result)
    case NoSuccess(message, _) => Right(message)
  }
}
