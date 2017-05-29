package com.excel.converter

import com.excel.converter.qsIntermediateProcessor.QsIntermediateProcessor
import org.apache.poi.ss.usermodel.Workbook
import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.notifications.errors.fatal.QsParseFatalError
import com.excel.converter.result.{IntermediateGlobalResult, IntermediateQuestionResult}

import scalaz.\/

/**
  * Created by Alosha on 10/22/2016.
  */
object IntermediateConverter {
  def apply(workbook: Workbook) : IntermediateGlobalResult = {

    val sheet = workbook.getSheetAt(0)
    println("Number of rows - " + sheet.getPhysicalNumberOfRows)
    val rowList = sheet.toRowList
    val rowsByQuestions = rowList.splitRowsByQuestions

//    println(rowsByQuestions.map(_.show).mkString("\r\n--------------------------------------\r\n"))

    val intermediateResults = rowsByQuestions.map(questionRows => {
      QsIntermediateProcessor(questionRows).map(_.json)
    })

    new IntermediateGlobalResult(intermediateResults)
  }
}
