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
import models.domain.{ PaginatedResult, Challenge }
import models.repo.ChallengeRepo

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton 
class ChallengeService @Inject()(challengeRepo: ChallengeRepo ) {
  def paginatedResult[T >: Challenge](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
  	  
	  for {
      tasks <- challengeRepo.findAll(limit, offset)
      size <- challengeRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getChallengeByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  	try {
  		for {
	      txs <- challengeRepo.findByDateRange(
	      	start.getEpochSecond, 
	      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	      	limit, 
	      	offset)
	      size <- challengeRepo.getSize()
	      hasNext <- Future(size - (offset + limit) > 0)
	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  	} catch {
  		case e: Throwable => Future(Json.obj("err" -> e.toString))
  	}
  }
 
  def getchallengeByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {

    try {
      for {
        txs <- challengeRepo.findByDaily(start.getEpochSecond, limit, offset)
        size <- challengeRepo.getSize()
        hasNext <- Future(size - (offset + limit) > 0)
      } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
    } catch {
      case e: Throwable => Future(Json.obj("err" -> e.toString))
    }
  }
}