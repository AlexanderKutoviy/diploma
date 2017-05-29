package com.excel.converter.notifications.errors.notFatal.fieldMatch

import com.excel.converter.notifications.{Location, NotifLocation}
import com.excel.converter.notifications.traits.Location
import org.apache.poi.ss.usermodel.Row

/**
  * Created by Alosha on 10/26/2016.
  */
class DuplicateField(val prevLocation: NotifLocation[Seq[Option[Row]]]) extends QsFieldMatchError {

  override def toString: String =
    s"This field was already processed(here: $prevLocation)"
}

object DuplicateFieldWithPrevDuplLocations {
  def apply(prevLocation : Seq[Option[Row]], location : Seq[Option[Row]]): DuplicateField with Location[ Seq[Option[Row]] ] = {
    new DuplicateField(Location(prevLocation)) with Location[ Seq[Option[Row]] ] {
      val tLocation = Location(location)
    }
  }

  def unapply(duplicateField: DuplicateField) : Option[ (Seq[Option[Row]], Seq[Option[Row]]) ] =
    duplicateField match {
      case withLocation : DuplicateField with Location[ Seq[Option[Row]] ] =>
        Some(withLocation.prevLocation.rawLocation, withLocation.tLocation.rawLocation)

      case _ =>
        None
    }
}