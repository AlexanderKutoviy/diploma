package com.excel.converter.field

import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.Row

/**
  * Created by Alosha on 9/30/2016.
  */
abstract class QsField {
  def tryFindMatchInRowQueue(rowQueue : Seq[Option[Row]]) : Option[(Seq[Option[Row]], Seq[Option[Row]])]
  def applyTransform(question : IntermediateQuestionResult, matchedRows: Seq[Option[Row]]) : IntermediateQuestionResult
}