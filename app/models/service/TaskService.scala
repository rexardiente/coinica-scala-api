package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.{ Instant, ZoneId }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, Task }
import models.repo.TaskRepo

@Singleton 
class TaskService @Inject()(taskRepo: TaskRepo ) {
  def paginatedResult[T >: Task](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
  	  
	  for {
      tasks <- taskRepo.findAll(limit, offset)
      size <- taskRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getTaskByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  	try {
  		for {
	      txs <- taskRepo.findByWeekly(
	      	start.getEpochSecond, 
	      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	      	limit, 
	      	offset)
	      size <- taskRepo.getSize()
	      hasNext <- Future(size - (offset + limit) > 0)
	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  	} catch {
  		case e: Throwable => Future(Json.obj("err" -> e.toString))
  	}
  }
  def getTaskByMonthly(currendate: Instant,  limit: Int, offset: Int): Future[JsValue] = {
	  val cmonth: java.time.Instant = currendate
      val cyear: java.time.Instant = currendate
  
     cmonth.atZone(ZoneId.systemDefault).getMonth()
     cyear.atZone(ZoneId.systemDefault).getYear()

  	try {
      for {
        txs <- taskRepo.findByMonthly(cmonth.getEpochSecond, cyear.getEpochSecond, limit, offset)
        size <- taskRepo.getSize()
        hasNext <- Future(size - (offset + limit) > 0)
      } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
    } catch {
      case e: Throwable => Future(Json.obj("err" -> e.toString))
    }
  
  }
  def getTaskByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {

    try {
      for {
        txs <- taskRepo.findByDaily(start.getEpochSecond, limit, offset)
        size <- taskRepo.getSize()
        hasNext <- Future(size - (offset + limit) > 0)
      } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
    } catch {
      case e: Throwable => Future(Json.obj("err" -> e.toString))
    }
  }
}