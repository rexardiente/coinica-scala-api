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
import models.domain.{ PaginatedResult, Task }
import models.repo.TaskRepo

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

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
	      txs <- taskRepo.findByDateRange(
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
  /*
   val cmonth: java.time.Instant = start
   val cyear: java.time.Instant = start
   val DATE_FORMAT = "EEE, MMM dd, yyyy h:mm a"
   
   val startdate: String = cmonth.atZone(ZoneId.systemDefault).getMonth().toString + "-01- " + cyear.atZone(ZoneId.systemDefault).getYear().toString +"T00:00:00.00Z"
   val enddate: String = cmonth.atZone(ZoneId.systemDefault).getMonth().toString + "-30- " + cyear.atZone(ZoneId.systemDefault).getYear().toString +"T00:00:00.00Z"
   val dateFormat = new SimpleDateFormat(DATE_FORMAT)
   dateFormat.parse(startdate)
   val start1 : Instant =  Instant.parse(startdate)
   val end1 : Instant =  Instant.parse(startdate)
  */
 def getTaskByMonthly(start: Instant,  end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
   
  // val start1 : java.time.Instant=  Instant.parse(startdate)
  // val end1 : java.time.Instant =  Instant.parse(startdate)
  	try {
  		for {
	      txs <- taskRepo.findByDateRange(
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