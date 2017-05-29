package com.excel.converter.jCode

import org.json4s.JsonAST.JString

import scala.util.matching.Regex
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz._, Scalaz._

class JCode(val code : String) extends JString(JCode.prefix + code + JCode.suffix)

object JCode {
  val prefix : String = "##JS##"
  val suffix : String = "##JS-END##"

  def prepareJs(js : String) : String =
    ("\"" + prefix + "(.+?)" + suffix + "\"").r.replaceAllIn(js, regRes => {
      parse("[\"" + regRes.group(1) + "\"]").asInstanceOf[JArray] match {
        case JArray(List(JString(code))) => code.replaceAll("\n", "\n" + " " * 9)
      }
    }).replaceAll(prefix, "").replaceAll(suffix, "").replaceAll("\\\\n", "\n" + " " * 9)

}
