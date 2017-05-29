package com.excel.converter.notifications.errors.fatal

import com.excel.converter.notifications.traits.Qtype

/**
  * Created by Alosha on 10/19/2016.
  */
trait QsParseFatalError {
  override def toString: String =
    "Fatal question parse error: " + super.toString
}

class QuestionTypeNotFound extends QsParseFatalError {
  override def toString: String =
    "Question type not found"
}

case class UnrecognizedQuestionType(tQtype : String) extends QsParseFatalError with Qtype {
  override def toString: String =
    "Unrecognized question type"
}
