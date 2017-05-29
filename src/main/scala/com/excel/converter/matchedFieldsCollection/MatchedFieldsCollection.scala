package com.excel.converter.matchedFieldsCollection

import com.excel.converter.field.QsField
import com.excel.converter.result.IntermediateQuestionResult
import org.apache.poi.ss.usermodel.Row
import org.json4s.JsonAST.JObject

import scalaz._
import Scalaz._
import com.excel.converter.notifications.Location._
import com.excel.converter.notifications.errors.notFatal.fieldMatch._
import com.excel.converter.notifications.traits.{Qid, Qtype}


class MatchedFieldsCollection private(
                                       val matchedFields : Vector[(QsField, Seq[Option[Row]])],
                                       private val matchedFieldsMap : Map[QsField, Seq[Option[Row]]],

                                       val errors : Vector[QsFieldMatchError]
) {

    def pushField(field : (QsField, Seq[Option[Row]])): MatchedFieldsCollection =
      new MatchedFieldsCollection(matchedFields :+ field, matchedFieldsMap + (field._1 -> field._2), errors)

    def pushUnrecognizedRow(row : Option[Row]) : MatchedFieldsCollection =
      new MatchedFieldsCollection(
        matchedFields,
        matchedFieldsMap,
        errors match {
          case init :+ UnrecognizedFieldWithLocation(rows) if rows.last.zip(row).exists({
            case (row1, row2) =>
              Math.abs(row1.getRowNum - row2.getRowNum) == 1
          }) =>
            init :+ UnrecognizedFieldWithLocation(rows :+ row)
          case _ =>
            errors :+ UnrecognizedFieldWithLocation(Vector(row))
        }
      )

    def pushDuplicateFieldError(prevLocation : Seq[Option[Row]], location : Seq[Option[Row]]): MatchedFieldsCollection =
      new MatchedFieldsCollection(
        matchedFields,
        matchedFieldsMap,
        errors :+ DuplicateFieldWithPrevDuplLocations(prevLocation, location)
      )

    def mapErrors(f : QsFieldMatchError  => QsFieldMatchError): MatchedFieldsCollection =
      new MatchedFieldsCollection(
        matchedFields,
        matchedFieldsMap,
        errors.map(f)
      )

    def applyMatches(initJson : JObject) : IntermediateQuestionResult = {

      val withAppliedFields = matchedFields.foldLeft(new IntermediateQuestionResult(initJson)) ( {
        case (accum : IntermediateQuestionResult, (matchedField : QsField, matchedRows : Seq[Option[Row]]) ) =>
          matchedField.applyTransform(accum, matchedRows)
      })

      withAppliedFields.pushErrors(errors)
    }

    def tryGetFieldLocation(field : QsField) : Option[Seq[Option[Row]]] =
      matchedFieldsMap.get(field)
}

object MatchedFieldsCollection {
  def empty =
    new MatchedFieldsCollection(Vector(), Map(), Vector())
}
