package com.excel.converter.result

import com.excel.converter.notifications.errors.notFatal.QsParseNotFatalError
import com.excel.converter.notifications.warnings.ParseWarning
import org.json4s.JsonAST.{JObject, JString}

/**
  * Created by Alosha on 10/19/2016.
  */
class IntermediateQuestionResult(
      val json: JObject,
      val errors: Vector[QsParseNotFatalError] = Vector(),
      val warnings: Vector[ParseWarning] = Vector()
) {
    def manipJson(f : JObject => JObject) =
      new IntermediateQuestionResult(
        f(json),
        errors,
        warnings
      )

    def pushError(error : QsParseNotFatalError) =
      new IntermediateQuestionResult(
        json,
        errors :+ error,
        warnings
      )

    def pushErrors(newErrors : Seq[QsParseNotFatalError]) =
      new IntermediateQuestionResult(
        json,
        errors ++ newErrors,
        warnings
      )

    def pushWarning(warning : ParseWarning) =
      new IntermediateQuestionResult(
        json,
        errors,
        warnings :+ warning
      )

    def getType : String =
      json \ "type" match {
        case JString(t) => t
      }
}
