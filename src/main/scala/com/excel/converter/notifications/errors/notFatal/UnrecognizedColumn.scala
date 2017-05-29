package com.excel.converter.notifications.errors.notFatal

/**
  * Created by Alosha on 10/26/2016.
  */
class UnrecognizedColumn extends QsParseNotFatalError
{
  override def toString: String =
    "Unrecognized column"
}
