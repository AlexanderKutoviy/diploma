package com.excel.converter.qsFieldsBag.postProcessors

import com.excel.converter.jCode.JCode
import com.excel.converter.result.IntermediateQuestionResult
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.native.JsonMethods._

import scala.util.matching.Regex

/**
  * Created by alosha on 3/28/17.
  */
object AliasesPostProcessor extends Function1[IntermediateQuestionResult, IntermediateQuestionResult] {
  override def apply(question: IntermediateQuestionResult): IntermediateQuestionResult =
    question.manipJson(json => {
      val aliases : List[JObject] = (json \ "aliases").asInstanceOf[JArray].arr.map(_.asInstanceOf[JObject])
      val codeLines = aliases.map(jObj => {
        val name = (jObj \ "name").asInstanceOf[JString].s
        val rawCond = (jObj \ "cond").asInstanceOf[JString].s
        val cond = rawCond match {
            case s if s.dropWhile(_.isWhitespace).startsWith("function") => s
            case s => pretty(render(JString(s)))
          }
        s"alias.defineOr('$name', $cond)"
      })
      JObject(
        "type" -> JString("JsCode"),
        "code" -> JString(codeLines.mkString("\n"))
      )
    })

}
