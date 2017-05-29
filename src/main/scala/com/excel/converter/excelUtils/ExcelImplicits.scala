package com.excel.converter.excelUtils

import org.apache.poi.ss.usermodel._

import scalaz._, Scalaz._

object ExcelImplicits {
  implicit def richCell(cell : Option[Cell]) : RichCell = RichCell(cell)
  implicit def richCell(cell : Cell) : RichCell = RichCell(cell.some)
  implicit def richRow(row : Option[Row]) : RichRow = RichRow(row)
  implicit def richRow(row : Row) : RichRow = RichRow(row.some)
  implicit def richRowList(rows : Seq[Option[Row]]) : RichRowList = RichRowList(rows)
  implicit def richSheet(sheet : Sheet) : RichSheet = RichSheet(sheet)
}
