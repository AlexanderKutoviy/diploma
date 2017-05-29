package com.excel.converter.excelUtils

import org.apache.poi.ss.usermodel.Cell
import org.json4s.JsonAST._
import ExcelImplicits._

import scalaz.{-\/, \/, \/-}

/**
  * Created by Alosha on 9/30/2016.
  */
case class RichCell(cell : Option[Cell]) {
  def stringValue : String = {
    cell match {
      case Some(nonNullCell) =>
        nonNullCell.getCellType match {
          case Cell.CELL_TYPE_STRING => nonNullCell.getStringCellValue
          case Cell.CELL_TYPE_NUMERIC  =>
            val num = nonNullCell.getNumericCellValue
            if(num % 1 == 0)
              num.toInt.toString
            else
              num.toString
          case Cell.CELL_TYPE_BOOLEAN => nonNullCell.getBooleanCellValue.toString
          case Cell.CELL_TYPE_BLANK => ""
        }
      case _ =>
          ""
    }
  }

  def jsonValue : JValue =
    cell match {
      case Some(nonNullCell) =>
        nonNullCell.getCellType match {
          case Cell.CELL_TYPE_BLANK =>
            JNull
          case Cell.CELL_TYPE_NUMERIC =>
            if (nonNullCell.getCellStyle.getDataFormatString.contains("%")) {
              // Detect Percent Values
              val value = nonNullCell.getNumericCellValue * 100
              import java.text.DecimalFormat
              val formatter = new DecimalFormat("#.###")
              JString(s"${formatter.format(value)}%")
            } else {
              val num = nonNullCell.getNumericCellValue
              if(num % 1 == 0)
                JInt(num.toInt)
              else
                JDouble(num)
            }
          case Cell.CELL_TYPE_BOOLEAN =>
            JBool(nonNullCell.getBooleanCellValue)
          case Cell.CELL_TYPE_STRING if List("+", "Yes").exists(_.equalsIgnoreCase(nonNullCell.getStringCellValue)) =>
            JBool.True
          case Cell.CELL_TYPE_STRING if List("-", "No").exists(_.equalsIgnoreCase(nonNullCell.getStringCellValue)) =>
            JBool.False
          case Cell.CELL_TYPE_STRING => JString(nonNullCell.getStringCellValue)
        }
      case _ =>
        JNull
    }

  def boolValue : String \/ Boolean = {
    val strVal = cell.stringValue
    if(strVal.isEmpty)
      \/-(false)
    else
      jsonValue match {
        case JBool(bool) => \/-(bool)
        case _ => -\/(strVal)
      }
  }

  def isBlank : Boolean =
    cell.forall(nonNullCell => nonNullCell.getCellType == Cell.CELL_TYPE_BLANK || nonNullCell.stringValue.isEmpty)

  def isCommented : Boolean =
    cell.exists(c => {
      val strVal = c.stringValue
      strVal.nonEmpty && strVal(0) == '#'
    })

  def showCellCoords : String = {
    cell match {
      case Some(notNullCell) =>
        s"${notNullCell.getRowIndex}:${notNullCell.getColumnIndex}"
      case None =>
        "Undefined cell coords (Null cell)"
    }
  }
}
