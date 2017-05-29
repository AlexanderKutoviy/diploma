package com.excel.converter.field

import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.result.IntermediateQuestionResult
import com.excel.converter.utils.stringTranslators.StringTranslator
import org.apache.poi.ss.usermodel.Row
import org.json4s.JsonAST.JObject

/**
  * Created by Alosha on 9/30/2016.
  */
class SimplePairField(translator : StringTranslator) extends QsField {

  def tryFindMatchInRowQueue(rowQueue : Seq[Option[Row]]) : Option[(Seq[Option[Row]], Seq[Option[Row]])] = {
    val label = rowQueue.head.cellAt(1).stringValue
    if(translator.predicate(label))
      Some(List(rowQueue.head), rowQueue.tail)
    else
      None
  }

  def applyTransform(questionJson : IntermediateQuestionResult, matchedRows: Seq[Option[Row]]) : IntermediateQuestionResult = {
    val label = matchedRows.head.cellAt(1).stringValue
    val propValue = matchedRows.head.cellAt(2).jsonValue
    questionJson.manipJson(_.merge(JObject(translator.transformer(label) -> propValue)))
  }

}

object SimplePairField {
    def apply( translator : StringTranslator ) =
        new SimplePairField(translator)
}