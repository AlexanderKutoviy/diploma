package com.excel.converter.notifications.traits

/**
  * Created by Alosha on 10/21/2016.
  */
trait Qtype {
  def tQtype : String

  abstract override def toString: String =
    s"${super.toString} ($tQtype)"

}
