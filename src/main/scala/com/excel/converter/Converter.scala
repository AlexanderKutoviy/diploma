package com.excel.converter

import com.excel.converter.result.FinalResult
import org.apache.poi.ss.usermodel.Workbook

import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz._


/**
  * Created by Alosha on 10/19/2016.
  */
class Converter {

  def filepathToJsonString(filename: String): String = {
    WorkbookFromFile(filename) match {
      case \/-(workbook) =>
        val finalRes = workbookToResult(workbook)
        pretty(render(finalRes.json))
      case -\/(error) =>
        pretty(render(JObject(
          "error" -> JString(error)
        )))
    }
  }

  def workbookToResult(workbook: Workbook): FinalResult = {

    val intermediateResults = IntermediateConverter(workbook)
    val finalResult = Finalizer(intermediateResults)

    finalResult
  }
}
