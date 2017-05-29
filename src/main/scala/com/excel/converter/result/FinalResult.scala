package com.excel.converter.result

import com.excel.converter.jCode.JCode
import com.excel.converter.notifications.errors.fatal.QsParseFatalError
import com.excel.converter.utils.JsPrettyPrint
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz._
import Scalaz._

class FinalResult(
                 private val intermResult : IntermediateGlobalResult
                 ) {


  def json: JObject =
    JObject(
      "js" -> JString(js),
      "notifications" -> JString(notifications)
    )

  val js: String = {
    s"""
       | // GENERATED FROM EXCEL FILE
       | // DO NOT MODIFY THIS FILE
       | // CONSIDER MODIFYING ORIGINAL EXCEL FILE
       | 
       | define(["require", "lib/GenericFormModel"], function (requirejs, GenericFormModel) {
       |   return function () {
       |       var qs = [];
       |
       |       var model = new GenericFormModel();
       |       var alias = requirejs("lib/AliasPlugin").fromModel(model);
       |
       |       ${jsPrettyGlobalNotifications(intermResult.globalNotifications)}
       |
       |       ${intermResult.getOrderedQuestions.map(JsPrettyPrint.apply).mkString("\r\n\r\n       ")}
       |
       |       var data = {
       |            "questions": qs
       |       };
       |
       |       model.setData(data)
       |       return model;
       |
       |    }
       | });
       |
    """.stripMargin |> JCode.prepareJs
  }



  private def jsPrettyGlobalNotifications(notifications : Seq[String]) : String = {
    import JsPrettyPrint._
    notifications.map(notif => s"/* $notif */").mkString.fixJsEOLs
  }


  val notifications : String = {
    Stream(
      notifFatalErrors,
      notifNotFatalErrors,
      notifWarnings,
      notifGlobalNotifications
    ).filter(_.nonEmpty).intersperse(List("")).flatten.mkString("\n")
  }

  private def notifFatalErrors : Seq[String] = {
    val fatalErrors =
      intermResult.questions.collect({
        case -\/(fatalError) =>
          fatalError
      })

    if(fatalErrors.isEmpty)
      Nil
    else
      "FATAL ERRORS:" +: fatalErrors.map(_.toString)

  }

  private def notifNotFatalErrors : Seq[String] = {
    val notFatalErrors =
      intermResult.questions.collect({
        case \/-(q) =>
          q
      }).flatMap(q => q.errors)

    if(notFatalErrors.isEmpty)
      Nil
    else
      "ERRORS:" +: notFatalErrors.map(_.toString)

  }

  private def notifWarnings : Seq[String] = {
    val warnings =
      intermResult.questions.collect({
        case \/-(q) =>
          q
      }).flatMap(q => q.warnings)

    if(warnings.isEmpty)
      Nil
    else
      "WARNINGS:" +: warnings.map(_.toString)

  }

  private def notifGlobalNotifications : Seq[String] = {
    val notif = intermResult.globalNotifications

    if(notif.isEmpty)
      Nil
    else
      "NOTIFICATIONS:" +: notif
  }
}
