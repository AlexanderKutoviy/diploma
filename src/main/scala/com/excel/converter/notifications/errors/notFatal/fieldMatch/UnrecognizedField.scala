package com.excel.converter.notifications.errors.notFatal.fieldMatch

import com.excel.converter.notifications.Location
import com.excel.converter.notifications.traits.Location
import org.apache.poi.ss.usermodel.Row

/**
  * Created by Alosha on 10/26/2016.
  */
class UnrecognizedField extends QsFieldMatchError
{
  override def toString: String =
    "Unrecognized field(s)"
}

object UnrecognizedFieldWithLocation {
  def apply(location : Seq[Option[Row]]): UnrecognizedField with Location[ Seq[Option[Row]] ] = {
    new UnrecognizedField with Location[ Seq[Option[Row]] ] {
      val tLocation = Location(location)
    }
  }

  def unapply(unrecognizedField: UnrecognizedField) : Option[ Seq[Option[Row]] ] =
    unrecognizedField match {
      case withLocation : Location[ Seq[Option[Row]] ] =>
        Some(withLocation.tLocation.rawLocation)

      case _ =>
        None
    }
}