package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

//case class Ranking(id: UUID, name : String, bets: Double, profit: Double, multiplieramount: Double, rankingcreated: Long)

//object Ranking{
//	implicit def implRanking = Json.format[Ranking]
//}
case class Ranking(
                     id: Int,
                     name: String,
                     bets: Double,
                     profit: Double,
					 multiplieramount: Double, 
				     rankingcreated: Long
                   ) {
      override def equals(that: Any): Boolean = true
    }

    object Ranking {    
      implicit object RankingFormat extends Format[Ranking] {
        def writes(ranking: Ranking): JsValue = {
          val rankingSeq = Seq(
            "id" -> JsNumber(ranking.id),
            "name" -> JsString(ranking.name),
            "bets" -> JsNumber(ranking.bets),
            "profit" -> JsNumber(ranking.profit),
			"multiplieramount" -> JsNumber(ranking.multiplieramount),
			"rankingcreated" -> JsNumber(ranking.rankingcreated)
          )
          JsObject(rankingSeq)
        }
//(json \ "total_count").as[Int],
        def reads(json: JsValue): JsResult[Ranking] = {    
          JsSuccess(Ranking(
            (json \ "id").as[Int],
            (json \ "name").as[String],
            (json \ "bets").as[Double],
			 (json \ "proft").as[Double],
			 (json \ "multiplieramount").as[Double],
            (json \ "rankingcreated").as[Long])
          )
        }
      }

      def tupled = (this.apply _).tupled
    }

