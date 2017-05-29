package com.excel.converter.excelUtils

import ExcelImplicits._
import org.apache.poi.ss.usermodel.{Cell, Row}

import scala.annotation.tailrec
import scalaz.DList

/**
  * Created by Alosha on 10/20/2016.
  */
case class RichRowList(rows : Seq[Option[Row]]) {
  def splitRowsByQuestions = {
    @tailrec
    def _splitRowsByQuestions(accum : Seq[Seq[Option[Row]]], toProcess : Seq[Option[Row]]): Seq[Seq[Option[Row]]] =
      toProcess match {
        case head::tail =>
          val (nextQ, queue) = tail.span(row => {
            val firstCell = row.cellAt(0)
            firstCell.isBlank || firstCell.isCommented
          })

          _splitRowsByQuestions((head::nextQ) +: accum, queue)

        case _ =>
          accum.reverse
      }
      _splitRowsByQuestions(Seq(), rows.dropWhile(_.isEmptyOrCommented))
  }

  def sliceRows(begin : Int = 0, end : Option[Int] = None) : Seq[Seq[Option[Cell]]] =
    rows.map(_.toCellList(begin, end))

  def transposedMatrix : Seq[Seq[Option[Cell]]] = {

    val colBegin =
      rows.collect({
        case Some(row) =>
          (row.getFirstCellNum to row.getLastCellNum).
            find(!row.cellAt(_).isBlank).
            getOrElse(Int.MaxValue)
      }).min

    val colEnd =
      rows.collect({
        case Some(row) =>
          (row.getFirstCellNum to row.getLastCellNum).
            reverse.
            find(!row.cellAt(_).isBlank).
            getOrElse(Int.MinValue)
      }).max

    val slicedRows = sliceRows(colBegin, Some(colEnd))

    slicedRows.transpose
  }

  def toColumnsWithHeaders : Map[Option[Cell], Seq[Option[Cell]]] =
    transposedMatrix.collect {
      case column@(header :: content) if column.exists(!_.isBlank) =>
        header -> content.filter(!_.isCommented)
    }.toMap

  def show : String =
    rows.map(row => row.show).mkString("\r\n")

}
