package com.excel.converter.excelUtils

import org.apache.poi.ss.usermodel.{Cell, Row}
import ExcelImplicits._
import scalaz._, Scalaz._

/**
  * Created by Alosha on 10/20/2016.
  */
case class RichRow(row : Option[Row]) {

  def cellAt(i : Int) : Option[Cell] =
    row.map(r => Option(r.getCell(i))).getOrElse(None)

  def allCellsAreBlank : Boolean = {
    row.exists(r =>
      r.getPhysicalNumberOfCells == 0 ||
      (r.getFirstCellNum to r.getLastCellNum).map(r.cellAt).forall(cell => cell.isBlank)
    )
  }

  def toCellList(implicit begin : Int = 0, end : Option[Int] = None) : Seq[Option[Cell]] = {
    row.map(r => {
        val prefix = (begin until r.getFirstCellNum).map(_ => None)

        val content_last_i : Int = end.map(Math.min(_, r.getLastCellNum)).getOrElse(r.getLastCellNum)

        val content = (Math.max(r.getFirstCellNum, begin) to content_last_i).map(i => r.cellAt(i) )

        val suffix  = (r.getLastCellNum + 1 to end.getOrElse(-1)).map(_ => None)

        (prefix ++ content ++ suffix).toList

    }).getOrElse(
      (begin to end.getOrElse(0)).map(_ => None)
    )
  }

  def show : String = {
    if(row == null)
      "NULL ROW"
    else
      toCellList.map(_.stringValue).mkString(" | ")
  }

  def showRowIndex : String = {
    row match {
      case Some(notNullRow) =>
        notNullRow.getRowNum.toString
      case None =>
        "Undefined index (Null row)"
    }
  }

  def getFirstCell : Option[Cell] = {
    row.flatMap(_.cellAt(0))
  }

  def isEmptyOrCommented : Boolean = {
      toCellList.find(cellOpt => !cellOpt.isBlank).forall(_.isCommented)
  }
}
