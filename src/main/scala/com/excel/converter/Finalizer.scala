package com.excel.converter

import com.excel.converter.notifications.errors.fatal.QsParseFatalError
import com.excel.converter.result.{FinalResult, IntermediateGlobalResult, IntermediateQuestionResult}
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz._
import Scalaz._

/**
  * Created by Alosha on 10/22/2016.
  */
object Finalizer {
  def apply(intermResult : IntermediateGlobalResult) : FinalResult = {
      new FinalResult(intermResult)
  }
}
