package com.excel.converter.result

import com.excel.converter.notifications.errors.fatal.QsParseFatalError
import org.json4s.JsonAST.{JField, JString}

import scalaz.{-\/, \/, \/-}
import org.json4s._
import org.json4s.native.JsonMethods._

/**
  * Created by Alosha on 10/22/2016.
  */
class IntermediateGlobalResult private (
    val questions : Seq[QsParseFatalError \/ IntermediateQuestionResult],
    val globalNotifications : Seq[String]
) {

    def this(questionParseResults : Seq[QsParseFatalError \/ IntermediateQuestionResult]) = {
      this(
        questionParseResults,
        {

            val qids : Seq[String] =
              for {
                \/-(q) <- questionParseResults
                JString(qid) <- q.json \ "id"
              } yield qid

            val duplicates =
              qids.groupBy(identity).mapValues(_.size).toList.filter(_._2 > 1)

            duplicates.map({
              case (qid : String, times : Int) =>
                s"Duplicate question id $qid: repeated ${times - 1} times"
            })
        }
      )

    }

    def getOrderedQuestions : Seq[QsParseFatalError \/ IntermediateQuestionResult] = {
//      questions.partition({
//        case \/-(question) => question.getType == "Aliases"
//        case -\/(_) => false
//      }) match {
//        case (aliases, rest) => aliases ++ rest
//      }
      questions
    }

}
