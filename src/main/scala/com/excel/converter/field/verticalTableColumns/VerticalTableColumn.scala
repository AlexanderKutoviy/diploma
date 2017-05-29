package com.excel.converter.field.verticalTableColumns

/**
  * Created by Alosha on 10/21/2016.
  */
trait VerticalTableColumn {
  val headerPredicate : String => Boolean
}
