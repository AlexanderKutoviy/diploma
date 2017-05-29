package com.excel.converter.field

import com.excel.converter.field.verticalTableColumns.{RowItemColumn, ShuffleColumn, VerticalTableColumn}
import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.{Cell, Row}
import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.notifications.Location
import com.excel.converter.notifications.traits.Location
import com.excel.converter.notifications.warnings.ParseWarning
import org.json4s.JsonAST.{JArray, JObject}

import scalaz._
import Scalaz._

/**
  * Created by Alosha on 10/21/2016.
  */
class VerticalTable(
                     tableHeaderVariants : Seq[String],
                     jsonProp : String,
                     columns : Seq[VerticalTableColumn],

                     postProcessor : Option[IntermediateQuestionResult => IntermediateQuestionResult] = None
                   ) extends QsField {

  def withPostProcessor(postProcessor : IntermediateQuestionResult => IntermediateQuestionResult) =
    new VerticalTable(tableHeaderVariants, jsonProp, columns, postProcessor.some)

  override def tryFindMatchInRowQueue(rowQueue: Seq[Option[Row]]): Option[(Seq[Option[Row]], Seq[Option[Row]])] = {
    val label = rowQueue.head.cellAt(1).stringValue

    tableHeaderVariants.find(label.equalsIgnoreCase) *>
      Some(rowQueue.span(!_.cellAt(1).isBlank))
  }

  override def applyTransform(question: IntermediateQuestionResult, matchedRows: Seq[Option[Row]]): IntermediateQuestionResult = {
    val tableRows = matchedRows.tail

    val columnsWithHeaders = tableRows.toColumnsWithHeaders.toList

    val groupedColumns: List[(VerticalTableColumn, (String, Seq[Option[Cell]]))] =
      columnsWithHeaders
        .map({
          case (header : Option[Cell], content : Seq[Option[Cell]]) =>
            val headerStr = header.stringValue
            val columnOpt = columns.find(_.headerPredicate(headerStr))
            columnOpt.orNull -> (headerStr, content)
        })

    val (undefinedColumns, definedColumns) =
      groupedColumns.partition(_._1 == null)

    val columnsByType =
      definedColumns.groupBy(_._1.getClass.getSimpleName)

    val rowColumns = columnsByType.getOrElse("RowItemColumn", List()).map(_.asInstanceOf[(RowItemColumn, (String, Seq[Option[Cell]]))])
    val shuffleColumns = columnsByType.getOrElse("ShuffleColumn", List()).map(_.asInstanceOf[(ShuffleColumn, (String, Seq[Option[Cell]]))])

    val processedRes = List(
      applyRowColumns(rowColumns),
      applyShuffleColumns(shuffleColumns)
    ).sequence[ResultState, Unit]
      .exec(question)

    val postProcessedRes = postProcessor.map(_(processedRes)).getOrElse(processedRes)

    postProcessedRes
  }

  type ResultState[T] = State[IntermediateQuestionResult, T]

  private def applyRowColumns(rowColumns : Seq[(RowItemColumn, (String, Seq[Option[Cell]]))]) : ResultState[Unit] = {
    if (rowColumns.nonEmpty)
      modify[IntermediateQuestionResult](res => res.manipJson(
        _.merge(JObject(jsonProp -> generateRowItems(rowColumns)))
      ))
    else
      modify(identity)
  }

  private def applyShuffleColumns(shuffleColumns: Seq[(ShuffleColumn, (String, Seq[Option[Cell]]))]) : ResultState[Unit] = {
      shuffleColumns match {
        case Nil =>
          modify(identity)

        case head :: tail =>
          if(tail.nonEmpty) {
            modify[IntermediateQuestionResult](res =>
              res.pushWarning(new ParseWarning("Multiple shuffle columns found for one table") with Location[ Seq[Option[Row]] ] {
                  val tLocation = Location(head._2._2.map(_.map(_.getRow)))
              })
            )
          }

          modify[IntermediateQuestionResult](res =>
            head._1.applyShuffle(res, head._2._2)
          )
      }
  }


  private def generateRowItems(rowItemColumns : Seq[(RowItemColumn, (String, Seq[Option[Cell]]))]): JArray = {

    val rows: Seq[Seq[(RowItemColumn, String, Option[Cell])]] =
      rowItemColumns.map({
        case (column : RowItemColumn, (headerStr : String, content : Seq[Option[Cell]])) =>
          content.toList.map((column, headerStr, _))
      }).transpose

    JArray(
        rows.map(
          _.foldLeft(JObject())({
            case (accum : JObject, (column : RowItemColumn, headerStr : String, cell : Option[Cell])) =>
              val newAccum = column.applyTransform(accum, cell.jsonValue, headerStr)
              newAccum
          })
        ).toList
    )
  }

}

object VerticalTable {
  def apply( translation: (String, String), columns: Seq[VerticalTableColumn]) =
    new VerticalTable(List(translation._1), translation._2, columns)

  def apply( translation: => (Seq[String], String), columns: Seq[VerticalTableColumn]) =
    new VerticalTable(translation._1, translation._2, columns)
}