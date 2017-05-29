package com.excel.converter.qsFieldsBag

import com.excel.converter.field.verticalTableColumns.{RowItemColumn, ShuffleColumn}
import com.excel.converter.field.{JsCodePairField, QsField, SimplePairField, VerticalTable}
import com.excel.converter.matchedFieldsCollection.MatchedFieldsCollection
import org.apache.poi.ss.usermodel.Row

import scala.annotation.tailrec
import scalaz.{-\/, \/, \/-}
import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.field
import com.excel.converter.notifications.errors.fatal.UnrecognizedQuestionType
import com.excel.converter.notifications.errors.notFatal.fieldMatch.UnrecognizedField
import com.excel.converter.qsFieldsBag.postProcessors.{CheckGroupMatrixPostProcessor, AliasesPostProcessor, RadioConjointPostProcessor}
import com.excel.converter.utils.stringTranslators.StringTranslator

/**
  * Created by Alosha on 10/19/2016.
  */
class QsFieldsBag private(val fields : Seq[QsField]) {

  def findMatches(questionMatchedRows : Seq[Option[Row]]) : MatchedFieldsCollection = {
    @tailrec
    def _findMatches(accum : MatchedFieldsCollection, questionRowsQueue : Seq[Option[Row]]) : MatchedFieldsCollection =
      questionRowsQueue match {
        case List() =>
          accum

        case head::tail if head.forall(row => row.toCellList.find(!_.isBlank).forall(_.isCommented)) =>
          _findMatches(accum, tail)

        case _ =>

          val matchedItems = fields collectFirst
            Function.unlift ((field : QsField) =>
              field.tryFindMatchInRowQueue(questionRowsQueue).map((_, field))
            )

          val (newQueue, newAccum) =
            matchedItems match {
              case Some( ((matchedRows, rowsRest), matchedField) ) =>
                val newAccum = accum.tryGetFieldLocation(matchedField) match {
                  case Some(alreadyMatchedLocation) =>
                    accum.pushDuplicateFieldError(alreadyMatchedLocation, matchedRows)
                  case None =>
                    accum.pushField(matchedField -> matchedRows)
                }

                ( rowsRest, newAccum)

              case None =>
                ( questionRowsQueue.tail, accum.pushUnrecognizedRow(questionRowsQueue.head) )
            }

          _findMatches(newAccum, newQueue)
      }

    _findMatches(MatchedFieldsCollection.empty, questionMatchedRows)
  }
}

object QsFieldsBag {

  import com.excel.converter.utils.stringTranslators.Implicits._

  val text = SimplePairField(List("Question text", "text") -> "text")
  val allowCustom = SimplePairField("Max custom options" -> "allowCustom")
  val customPlaceholder = SimplePairField(List("Custom options placeholders", "Custom option placeholder") -> "customPlaceholder")
  val optional = SimplePairField("Optional" -> "optional")

  val cond = SimplePairField(Seq("Condition", "Cond") -> "cond")
  val block = JsCodePairField("Block" -> "cond", block => s"isBlock.bind(null, $block)")
  val jsProp = JsCodePairField(
    new StringTranslator(predicate = (s: String) => (s.length() > 3) && s.substring(0, 3).equalsIgnoreCase("js-"), transformer = _.substring(3))
  )

  val items =
    VerticalTable(Seq("Answers", "Rows", "Items") -> "items", List(
        RowItemColumn("Code" -> "id"),
        RowItemColumn("Text" -> "text"),
        RowItemColumn(new StringTranslator(s =>
             s.sliding("Alias".length).exists("Alias".equalsIgnoreCase)
            || s.sliding("Define".length).exists("Define".equalsIgnoreCase),
          _ => "defineOr"
        )),
        new ShuffleColumn
    ))

  val paramTitles =
    VerticalTable(Seq("Columns", "Headers", "Legend") -> "paramTitles", Seq(
      RowItemColumn("Code" -> "id"),
      RowItemColumn(Seq("Label", "Text") -> "text"),
      RowItemColumn("Legend" -> "legend")
    ))

  val bulkRadioImagedTitles =
    VerticalTable("Columns" -> "paramTitles", Seq(
      RowItemColumn("Code" -> "id"),
      RowItemColumn("Legend" -> "legend")
    ))

  val imagedItems =
    VerticalTable(Seq("Answers", "Rows") -> "items", Seq(
      RowItemColumn("Code" -> "id"),
      RowItemColumn("Text" -> "text"),
      RowItemColumn("Image" -> "img"),
      new ShuffleColumn
    ))

  val bestWorstParamTitles =
    VerticalTable("Columns" -> "paramTitles", List(
      RowItemColumn("Code" -> "id"),
      RowItemColumn("Legends" -> "text")
    ))

  val fieldsByType : Map[String, Seq[QsField]] = {
    Map(
      "JsCode" -> Seq(
        JsCodePairField("Code" -> "code")
      ),
      "Info" -> Seq(
        text
      ),
      "CheckGroup" -> Seq(
        text,
        optional,
        allowCustom,
        customPlaceholder,
        items,
        SimplePairField(
          new StringTranslator(
            s => s.sliding("Min".size).exists("Min".equalsIgnoreCase)
              && s.sliding("Answer".size).exists("Answer".equalsIgnoreCase),
            _ => "minAnswersCount"
          )
        ),
        SimplePairField(
          new StringTranslator(
            s => s.sliding("Max".size).exists("Max".equalsIgnoreCase)
              && s.sliding("Answer".size).exists("Answer".equalsIgnoreCase),
            _ => "maxAnswersCount"
          )
        )
      ),
      "RadioGroup" -> Seq(
        text,
        optional,
        allowCustom,
        customPlaceholder,
        items
      ),
      "BulkRadio" -> Seq(
        text,
        optional,
        items,
        paramTitles
      ),
      "BulkCheck" -> Seq(
        text,
        optional,
        items,
        paramTitles
      ),
      "RadioGroupImaged" -> Seq(
        text,
        optional,
        imagedItems
      ),
      "CheckGroupImaged" -> Seq(
        text,
        optional,
        imagedItems
      ),
      "BulkRadioImaged" -> Seq(
        text,
        optional,
        bulkRadioImagedTitles,
        imagedItems
      ),
      "InputText" -> Seq(
        text,
        optional,
        SimplePairField("Textarea" -> "textarea"),
        SimplePairField("Placeholder" -> "placeholder")
      ),
      "BestWorst" -> Seq(
        text,
        optional,
        SimplePairField("Max best cnt" -> "maxBestCnt"),
        SimplePairField("Max worst cnt" -> "maxWorstCnt"),
        bestWorstParamTitles,
        items
      ),
      "Percent" -> Seq(
        text,
        optional,
        SimplePairField("Big items" -> "big-items"),
        SimplePairField(Seq("Can have no answer", "No answer") -> "no-answer"),
        items
      ),
      "CheckGroupMatrix" -> Seq(
        text,
        optional,
        SimplePairField("Max Answers Per Group" -> "maxAnswersPerGroup"),
        VerticalTable("Groups" -> "_groups", Seq(
          RowItemColumn("ID" -> "groupId"),
          RowItemColumn("Header" -> "header")
        )),
        VerticalTable(Seq("Items", "Rows") -> "_items", Seq(
          RowItemColumn("Code" -> "id"),
          RowItemColumn("Group" -> "group"),
          RowItemColumn("Item text" -> "text")
          // TODO new ShuffleColumn
        )).withPostProcessor(CheckGroupMatrixPostProcessor)
      ),
      "DropDownGroup" -> Seq(
        text,
        optional,
        paramTitles,
        items
      ),
      "Hierarchy" -> Seq(
        text,
        optional,
        SimplePairField("Single-column" -> "single-column"),
        paramTitles,
        items
      ),
      "RadioConjoint" -> Seq(
        text,
        optional,
        VerticalTable("Items" -> "items", Seq(
          RowItemColumn("Code" -> "id"),
          RowItemColumn(StringTranslator.id)
        )).withPostProcessor(RadioConjointPostProcessor)
      ),
      "Aliases" -> Seq(
        VerticalTable("Aliases" -> "aliases", Seq(
          RowItemColumn(new StringTranslator(s => s.sliding("Name".length).exists("Name".equalsIgnoreCase), _ => "name")),
          RowItemColumn(new StringTranslator(s => s.sliding("Cond".length).exists("Cond".equalsIgnoreCase), _ => "cond"))
        )).withPostProcessor(AliasesPostProcessor)
      )
    ).mapValues(cond +: block +: jsProp +: _)
  }

  def byQuestionType(qType : String): UnrecognizedQuestionType \/ QsFieldsBag =
    fieldsByType.get(qType) match {
      case Some(questionFields) =>
        \/-(new QsFieldsBag(questionFields))
      case None =>
        -\/(UnrecognizedQuestionType(qType))
    }
}