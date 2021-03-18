package models.service

import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.{ Instant, ZoneId }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, RankingHistory }
import models.repo.RankingHistoryRepo

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton
class RankingService @Inject()(rankingHistoryRepo: RankingHistoryRepo ) {
  def paginatedResult[T >: RankingHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- rankingHistoryRepo.findAll(limit, offset)
      size <- rankingHistoryRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // def getRankingByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  // 	try {
  // 		for {
	 //      txs <- rankingRepo.findByDateRange(
	 //      	start.getEpochSecond,
	 //      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	 //      	limit,
	 //      	offset)
	 //      size <- rankingRepo.getSize()
	 //      hasNext <- Future(size - (offset + limit) > 0)
	 //    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  // 	} catch {
  // 		case e: Throwable => Future(Json.obj("err" -> e.toString))
  // 	}
  // }

  // def getRankingByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {

  //   try {
  //     for {
  //       txs <- rankingRepo.findByDaily(start.getEpochSecond, limit, offset)
  //       size <- rankingRepo.getSize()
  //       hasNext <- Future(size - (offset + limit) > 0)
  //     } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  //   } catch {
  //     case e: Throwable => Future(Json.obj("err" -> e.toString))
  //   }
  // }
}