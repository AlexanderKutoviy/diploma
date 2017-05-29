package com.excel.converter.qsFieldsBag.postProcessors

import com.excel.converter.result.IntermediateQuestionResult
import org.json4s.JsonAST.{JArray, JField, JObject}
import org.json4s._

import scalaz.Scalaz._

/**
  * Created by Alosha on 11/4/2016.
  */
object CheckGroupMatrixPostProcessor extends Function1[IntermediateQuestionResult, IntermediateQuestionResult] {
  override def apply(question: IntermediateQuestionResult): IntermediateQuestionResult =
    question.manipJson(json => {

      val _itemsByGroups =
        (json \ "_items" match {
          case JArray(items) =>
            items.groupBy({
              case item : JObject =>
                item \ "group"
            })
        }).mapValues(_ map {
          case x : JObject => JObject(x filterField {
            case JField(prop, _) if prop == "id" || prop == "text" => true
            case _ => false
          })
        })

      val items = JArray(
        json \ "_groups" match {
          case JArray(groups) =>
            groups.map({
              case JObject(List(("groupId", groupId), ("header", groupHeader))) =>

                JObject(
                  ("id", groupId),
                  ("header", groupHeader),
                  ("items",
                    JArray(_itemsByGroups(groupId))
                    )
                )
            })
        }
      )

      json.merge(JObject("items" -> items)).removeField({
        case (prop, _) if prop == "_groups" || prop == "_items" => true
        case _ => false
      }) match {
        case res : JObject => res
      }
    })
}
