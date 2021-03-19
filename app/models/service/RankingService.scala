package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, RankingHistory }
import models.repo.RankingHistoryRepo

@Singleton
class RankingService @Inject()(rankingHistoryRepo: RankingHistoryRepo ) {
  def paginatedResult[T >: RankingHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- rankingHistoryRepo.findAll(limit, offset)
      size <- rankingHistoryRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // if date is None then it will be 24hrs
  def getRankingByDate(date: Option[Instant]): Future[Option[RankingHistory]] = {
    if (date == None) {
      val now: LocalDate = LocalDate.now()
      val startOfDay: Instant = now.atStartOfDay(ZoneId.systemDefault).plusDays(-1).toInstant()
      rankingHistoryRepo.findByDateRange(startOfDay)

    } else {
      val startOfDay: Instant = LocalDateTime
                                  .ofInstant(date.get, ZoneOffset.UTC)
                                  .toLocalDate()
                                  .atStartOfDay(ZoneId.systemDefault)
                                  .toInstant()
      rankingHistoryRepo.findByDateRange(startOfDay)
    }
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