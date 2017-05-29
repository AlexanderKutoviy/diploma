package com.excel.converter.field.verticalTableColumns

import com.excel.converter.utils.stringTranslators.StringTranslator
import org.json4s.JsonAST.{JNull, JObject, JValue}

/**
  * Created by Alosha on 10/21/2016.
  */
class RowItemColumn(
  val headerPredicate : String => Boolean,
  val headerToItemPropTransformer: String => String
) extends VerticalTableColumn {

  def applyTransform(rowItem : JObject, cellVal : JValue, header : String) : JObject =
    cellVal match {
      case JNull => rowItem
      case _ => rowItem.merge(JObject(headerToItemPropTransformer(header) -> cellVal))
    }

}

object RowItemColumn {
  def apply( headerToPropTranslator : StringTranslator ): RowItemColumn =
      new RowItemColumn(headerToPropTranslator.predicate, headerToPropTranslator.transformer)

}