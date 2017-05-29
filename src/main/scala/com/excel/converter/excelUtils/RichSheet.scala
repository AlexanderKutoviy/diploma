package com.excel.converter.excelUtils

import org.apache.poi.ss.usermodel.{Row, Sheet}
import ExcelImplicits._

/**
  * Created by Alosha on 10/20/2016.
  */
case class RichSheet(sheet : Sheet) {
  def toRowList : List[Option[Row]] = {
    for(i <- sheet.getFirstRowNum to sheet.getLastRowNum) yield {
      Option(sheet.getRow(i))
    }
  }.toList
}
