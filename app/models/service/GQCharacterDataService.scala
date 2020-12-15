package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{ JsValue, Json, JsNull }
import models.domain.PaginatedResult
import models.domain.eosio._
// import models.repo.GQCharacterDataRepo

// @Singleton
// class GQCharacterDataService @Inject()(characterDataRepo: GQCharacterDataDAO) extends utils.CommonImplicits {
//   def getTxByDateRange(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
//   	try {
//   		for {
// 	      txs <- characterDataRepo.getByDate(
// 	      	start.getEpochSecond, 
// 	      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond + 86400),  // Add 24hrs as default Instant.ofEpochSecond(start.getEpochSecond + 86400).getEpochSecond
// 	      	limit, 
// 	      	offset)
// 	      size <- characterDataRepo.getSize()
// 	      hasNext <- Future(size - (offset + limit) > 0)
// 	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
//   	} catch {
//   		case e: Throwable => Future(Json.obj("err" -> e.toString)) // need some upgrades
//   	}
//   }

//   def getByTxTraceID(traceID: String): Future[JsValue] =
//   	characterDataRepo
//   		.getByID(traceID)
//   		.map(_.map(Json.toJson(_)).getOrElse(JsNull))
// }