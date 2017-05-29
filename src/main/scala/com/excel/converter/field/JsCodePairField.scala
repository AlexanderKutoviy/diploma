package com.excel.converter.field

import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.Row
import org.json4s.JsonAST.JObject
import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.jCode.JCode
import com.excel.converter.utils.stringTranslators.StringTranslator

import scalaz._
import Scalaz._

class JsCodePairField(labelTranslator: StringTranslator, val valueTransformer : String => String) extends SimplePairField(labelTranslator) {

  override def applyTransform(questionJson : IntermediateQuestionResult, matchedRows: Seq[Option[Row]]) : IntermediateQuestionResult = {
    val label : String = matchedRows.head.cellAt(1).stringValue
    val propValue : String = matchedRows.head.cellAt(2).stringValue |> valueTransformer
    questionJson.manipJson(json => {
      val prevCond : Option[String] = json.findField(field => labelTranslator.predicate(field._1)).map(_._2.asInstanceOf[JCode].code)
      val newCond : String = prevCond.map(cnd => s"$cnd && $propValue").getOrElse(propValue)
      json.merge(
        JObject(
          labelTranslator.transformer(label) -> new JCode(newCond)
        )
      )
    })
  }

}

object JsCodePairField {

  def apply( labelTranslator: StringTranslator, transform : String => String = identity) =
    new JsCodePairField(labelTranslator, transform)
}
