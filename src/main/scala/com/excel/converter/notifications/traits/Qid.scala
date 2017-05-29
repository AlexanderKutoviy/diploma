package com.excel.converter.notifications.traits

/**
  * Created by Alosha on 10/21/2016.
  */
trait Qid {
  def tQid : String

  abstract override def toString: String =
    s"${super.toString} for question '$tQid'"
}
