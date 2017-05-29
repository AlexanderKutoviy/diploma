package com.excel.converter.notifications.traits

import com.excel.converter.notifications.NotifLocation
import org.apache.poi.ss.usermodel.{Cell, Row}
import com.excel.converter.excelUtils.ExcelImplicits._

/**
  * Created by Alosha on 10/20/2016.
  */
trait Location[T] {

  def tLocation : NotifLocation[T]

  abstract override def toString : String =
    s"${super.toString} at ${tLocation.toString}"
}