package com.excel.converter.utils

import com.excel.converter.notifications.errors.fatal.QsParseFatalError
import com.excel.converter.result.IntermediateQuestionResult
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz.{-\/, \/, \/-}


object JsPrettyPrint {
    def apply : \/[QsParseFatalError, IntermediateQuestionResult] => String = {
      case \/-(q) if q.getType == "JsCode" =>
        (q.json \ "code").asInstanceOf[JString] match {
          case JString(code) => code.fixJsEOLs
        }
      case \/-(q) =>
        jsPrettyQuestion(q)
      case -\/(error) =>
        jsPrettyFatalError(error)
    }

    private def jsPrettyQuestion(question : IntermediateQuestionResult) : String = {
      (
        ( if(question.errors.isEmpty)
          List()
        else
          question.errors.map(error => s"/* ${error.toString.fixJsEOLs(+2)} */ ${jsEOL(0)}")
          ) ++
          List(s"qs.push(${prettyJson(question.json)})")
        ).mkString
    }

    private def prettyJson(question : JObject) : String = {
      pretty(render(question)).fixJsEOLs
    }

    private def jsPrettyFatalError(error : QsParseFatalError) : String = {
      error.toString
    }

    def jsEOL(implicit delta : Int = 0) : String = "\n" + " " * (7 + delta)

    implicit class JsStringOps(str : String) {
      def fixJsEOLs(implicit delta : Int = 0) : String = str.split("\n").mkString(jsEOL(delta))
    }
}
