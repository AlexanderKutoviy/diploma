package com.excel.converter.qsFieldsBag.postProcessors

import com.excel.converter.result.IntermediateQuestionResult
import org.json4s.JsonAST.{JArray, JObject, JString}

/**
  * Created by Alosha on 11/4/2016.
  */
object RadioConjointPostProcessor extends Function1[IntermediateQuestionResult, IntermediateQuestionResult] {
  override def apply(question: IntermediateQuestionResult): IntermediateQuestionResult = question.manipJson(json => {
    (json mapField {
      case ("items", JArray(items)) => ("items", JArray(items map {
        case item : JObject => item mapField {
          case (propImg, jUrl) if propImg.takeRight(7).equalsIgnoreCase("(image)") =>
            (propImg.dropRight(7).trim, JObject("img" -> jUrl))
          case (prop, JString(url)) if url.takeRight(7).equalsIgnoreCase("(image)") =>
            (prop, JObject("img" -> JString(url.dropRight(7).trim)))
          case x => x
        }
      }))
      case x => x
    }).asInstanceOf[JObject].merge(JObject(
      "properties" ->
        (json \ "items" match {
          case JArray(items) => {
            implicit val formats = org.json4s.DefaultFormats
            JArray(items.headOption.map(_.extract[Map[String, Any]].keys.map({
              case propImg if propImg.takeRight(7).equalsIgnoreCase("(image)") =>
                propImg.dropRight(7).trim
              case x => x
            }).toList.map(JString)).getOrElse(Nil))
          }
        })
    ))
  })
}
