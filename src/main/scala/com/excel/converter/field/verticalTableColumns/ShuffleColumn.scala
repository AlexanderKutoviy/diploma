package com.excel.converter.field.verticalTableColumns

import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.Cell
import org.json4s.JsonAST._

import scalaz._
import Scalaz._
import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.notifications.errors.notFatal.{Expected, UnexpectedCellValue}

/**
  * Created by Alosha on 10/22/2016.
  */
class ShuffleColumn(

) extends VerticalTableColumn {

  val headerPredicate : String => Boolean = "Shuffle".==

  def applyShuffle(aResult : IntermediateQuestionResult, column : Seq[Option[Cell]]) : IntermediateQuestionResult =
    parseColumn(column).run(aResult) match {
      case (result, columnValues : Seq[Boolean]) =>
        if(columnValues.forall(!_))
          result
        else
          result.manipJson(_.merge(JObject("shuffleItems" -> generateShuffle(columnValues))))
    }

  type ResultState[T] = State[IntermediateQuestionResult, T]

  def parseColumn(column : Seq[Option[Cell]]): ResultState[List[Boolean]] =
    column.map(
      _.boolValue match {
        case \/-(bool) =>
          State[IntermediateQuestionResult, Boolean] (r => (r, bool))

        case -\/(unexpectedValue) =>
          State[IntermediateQuestionResult, Boolean] (r => (
            r.pushError(UnexpectedCellValue(Expected.Bool, unexpectedValue)),
            false
          ))
      }).toList.sequence[ResultState, Boolean]

  def generateShuffle(column : Seq[Boolean]) : JValue = {
    if (column.forall(_ == true))
      JBool.True
    else
      tryMatchExceptFirst(column).
        map(x => JObject("exceptFirst" -> JInt(x))).
          orElse(
              tryMatchExceptLast(column).
                map(x => JObject("exceptLast" -> JInt(x)))
          ).
            getOrElse({
                val excepts = matchExcepts(column)
                JObject("except" -> JArray(excepts.map(JInt(_)).toList))
            })
  }

  def tryMatchExceptFirst(column : Seq[Boolean]) : Option[Int] = {
    column.zipWithIndex.dropWhile(!_._1) match {
      case hd :: tl if tl.forall(_._1) =>
        Some(hd._2)
      case _ =>
        None
    }
  }

  def tryMatchExceptLast(column : Seq[Boolean]) : Option[Int] = {
    tryMatchExceptFirst(column.reverse)
  }

  def matchExcepts(column : Seq[Boolean]) : Seq[Int] = {
    column.zipWithIndex.filterNot(_._1).map(_._2)
  }
}

