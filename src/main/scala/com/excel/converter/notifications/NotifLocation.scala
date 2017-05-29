package com.excel.converter.notifications

import org.apache.poi.ss.usermodel.{Cell, Row}
import com.excel.converter.excelUtils.ExcelImplicits._

/**
  * Created by Alosha on 10/20/2016.
  */

abstract class NotifLocation[T] {
  val rawLocation : T
  override def toString: String
}

object Location {
  def apply(cell : Option[Cell]) = new NotifLocation[Option[Cell]] {
      override val rawLocation: Option[Cell] = cell

      override def toString: String =
        s"cell ${cell.showCellCoords} - '${rawLocation.stringValue}'"
    }

  def apply(row : => Option[Row]) = new NotifLocation[Option[Row]] {
      val rawLocation : Option[Row] = row

      override def toString: String =
        s"row ${rawLocation.showRowIndex}: ${rawLocation.show}"
    }

  def apply(rowGroup : Seq[Option[Row]]) : NotifLocation[Seq[Option[Row]]] = new NotifLocation[Seq[Option[Row]]] {
      override val rawLocation: Seq[Option[Row]] = rowGroup

      require(rawLocation.nonEmpty)

      override def toString: String =
        rawLocation match {
          case Seq(singleRow) =>
            (apply(singleRow) : NotifLocation[Option[Row]]).toString

          case head +: tail =>
            s"rows ${rawLocation.head.showRowIndex}-${rawLocation.last.showRowIndex}: ${head.show} ..."
        }
    }
}