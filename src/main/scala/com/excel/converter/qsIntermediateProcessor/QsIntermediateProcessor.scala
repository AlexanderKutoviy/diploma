package com.excel.converter.qsIntermediateProcessor

import com.excel.converter.excelUtils.ExcelImplicits._
import com.excel.converter.excelUtils.RichCell
import com.excel.converter.notifications.{Location, NotifLocation}
import com.excel.converter.qsFieldsBag.{QsFieldsBag}
import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.{Cell, Row}
import org.json4s.JsonAST.{JObject, JString}
import com.excel.converter.notifications.Location._
import com.excel.converter.notifications.errors._
import com.excel.converter.notifications.traits.{Location, Qid, Qtype}
import com.excel.converter.notifications.Location._
import com.excel.converter.notifications.errors.fatal.{QsParseFatalError, QuestionTypeNotFound, UnrecognizedQuestionType}
import com.excel.converter.notifications.errors.notFatal.fieldMatch.{DuplicateField, DuplicateFieldWithPrevDuplLocations, UnrecognizedField, UnrecognizedFieldWithLocation}

import scalaz._

/**
  * Created by Alosha on 9/30/2016.
  */
object QsIntermediateProcessor {
  def apply(rows: Seq[Option[Row]]): QsParseFatalError \/ QsIntermediateProcessor = {
    val qid =
      rows.head.cellAt(0).stringValue

    val typeRow::contentRows =
      rows.dropWhile(_.cellAt(1).isBlank)

    {
      for {
        qType <- typeRowToType(typeRow)
        fieldsBag <- QsFieldsBag.byQuestionType(qType)
      } yield
        new QsIntermediateProcessor(
          qid,
          qType,
          fieldsBag,
          contentRows
        )
    }.leftMap({
        case qtypeNotFound : QuestionTypeNotFound with Location[Cell] =>
          new QuestionTypeNotFound with Qid with Location[Cell] {
            val tQid = qid
            val tLocation = qtypeNotFound.tLocation
          }

        case UnrecognizedQuestionType(qtype) =>
          new UnrecognizedQuestionType(qtype) with Qid with Location[Option[Cell]] {
            val tQid = qid
            val tLocation = Location(typeRow.cellAt(2))
          }
    })
  }

  def typeRowToType : Option[Row] => QsParseFatalError \/ String = {
    case typeRow if typeRow.cellAt(1).stringValue.equalsIgnoreCase("Type") =>
      \/-(typeRow.cellAt(2).stringValue)

    case invalidRow =>
      -\/(new QuestionTypeNotFound with Location[Option[Cell]] {
            val tLocation = Location(invalidRow.cellAt(1))
        })

  }
}

class QsIntermediateProcessor private(
                                  qid : String,
                                  qType : String,
                                  fieldsBag : QsFieldsBag,
                                  contentRows : List[Option[Row]]
) {

  val json : IntermediateQuestionResult = {
    val matchedFieldsCollection = fieldsBag.findMatches(contentRows)

    val initJson = JObject(List(
        "id" -> JString(qid),
        "type" -> JString(qType)
      )
    )

    matchedFieldsCollection
      .mapErrors({
        case UnrecognizedFieldWithLocation(location) =>
          new UnrecognizedField with Qid with Qtype with Location[ Seq[Option[Row]] ] {
            val tQid = qid
            val tQtype = qType
            val tLocation = Location(location)
          }
        case DuplicateFieldWithPrevDuplLocations(prevLocation, location) =>
          new DuplicateField(Location(prevLocation)) with Qid with Qtype with Location[ Seq[Option[Row]] ] {
            val tQid = qid
            val tQtype = qType
            val tLocation = Location(location)
          }
        case kek =>
          println("kek")
          kek
      }).
      applyMatches(initJson)
  }

}