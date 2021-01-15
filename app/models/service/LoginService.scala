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
import models.domain.{ PaginatedResult, Login }
import models.repo.LoginRepo

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton 
class LoginService @Inject()(loginRepo: LoginRepo ) {
  def paginatedResult[T >: Login](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
  	  
	  for {
      tasks <- loginRepo.findAll(limit, offset)
      size <- loginRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getLoginByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  	try {
  		for {
	      txs <- loginRepo.findByDateRange(
	      	start.getEpochSecond, 
	      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	      	limit, 
	      	offset)
	      size <- loginRepo.getSize()
	      hasNext <- Future(size - (offset + limit) > 0)
	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  	} catch {
  		case e: Throwable => Future(Json.obj("err" -> e.toString))
  	}
  }
 
  def getloginByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {

    try {
      for {
        txs <- loginRepo.findByDaily(start.getEpochSecond, limit, offset)
        size <- loginRepo.getSize()
        hasNext <- Future(size - (offset + limit) > 0)
      } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
    } catch {
      case e: Throwable => Future(Json.obj("err" -> e.toString))
    }
  }
}