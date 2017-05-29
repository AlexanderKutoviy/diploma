package com.excel.converter.notifications.errors.notFatal

/**
  * Created by Alosha on 10/26/2016.
  */
case class UnexpectedCellValue(
  expected : String,
  actual : String
) extends QsParseNotFatalError {
  override def toString: String =
    s"Unexpected cell value. Expected: $expected; actual: $actual"
}

object Expected {
  def Bool : String =
    "+, -, Yes, No or empty cell"

}