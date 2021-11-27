package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneId, ZoneOffset }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import play.api.libs.json._
import models.domain.{ PaginatedResult, Task, DailyTask, TaskHistory, TaskGameInfo }
import models.repo.{ TaskRepo, TaskHistoryRepo, DailyTaskRepo }

@Singleton
class TaskService @Inject()(
                          taskRepo: TaskRepo,
                          taskHistoryRepo: TaskHistoryRepo,
                          dailyTaskRepo: DailyTaskRepo
                          ) {
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC
  def paginatedResult[T >: Task](limit: Int, offset: Int): Future[PaginatedResult[T]] = {

	  for {
      tasks <- taskRepo.withLimit(limit, offset)
      size <- taskRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getTodayTaskUpdates(user: UUID, gameID: UUID): Future[Option[DailyTask]] = {
    dailyTaskRepo.getTodayTaskByUserAndGame(user, gameID)
  }

  def getMonthlyTaskUpdates(user: UUID, gameID: UUID): Future[Seq[TaskHistory]] = {
    val startOfDay: LocalDate = LocalDate.now()
    val start: Instant = startOfDay.atStartOfDay().withDayOfMonth(1).toInstant(ZoneOffset.UTC)
    val end: Instant = startOfDay.atStartOfDay().withDayOfMonth(startOfDay.lengthOfMonth()).toInstant(ZoneOffset.UTC)

    for {
      history <- taskHistoryRepo.getMonthlyTaskByUserAndGame(user, gameID, start, end)
      processed <- Future.successful(mergeSeqTaskHistory(history))
    } yield (processed)
  }
  // TODO: need proper testing
  def mergeSeqTaskHistory(sequence: Seq[TaskHistory]) = {
    val newTempID: UUID = UUID.randomUUID
    sequence.foldRight(List.empty[TaskHistory]) {
      // case (TaskHistory(a, b, c, d, e, f, g), TaskHistory(h, i, j, k, l, m, n) :: list) if (c == j && d == k) =>
      case (TaskHistory(a, b, c, d, e, f, g), TaskHistory(h, i, j, k, l, m, n) :: list) =>
        TaskHistory(newTempID, b, c, d, (e + l), f, n) :: list
      case (other, list) => other :: list
    }
  }
  def getTodaysTasks(id: UUID): Future[Option[Task]] = {
    for {
      // due to timezon difference from server, result are not reflecting on users query..
      // to avoid this situation, directly get the latest task
      hasTask <- taskRepo.getLatestTask
      updatedTask <- f[Task]{
        hasTask.map { task =>
          // map task tasks to update each game progress.
          val aPromise = Promise[Future[Seq[TaskGameInfo]]]()
          val updateSeq: Future[Seq[TaskGameInfo]] = Future.sequence({task.tasks.map { game =>
            dailyTaskRepo
              .getTodayTaskByUserAndGame(id, game.game.id)
              .map { hasDailyTask =>
                // return default if has no daily tasks
                hasDailyTask.map { dailyTask =>
                  val currentTask: Option[TaskGameInfo] = task.tasks.find(_.game.id == dailyTask.game_id)
                  val updatedTask: Option[TaskGameInfo] = currentTask.map(_.copy(progress = Some(dailyTask.game_count)))
                  // remove existing task on the list.
                  updatedTask.get
                }.getOrElse(game)
              }
          }})
          // wait the process to be completed
          aPromise.success(updateSeq)
          // get the future result from promise action
          // flatten to convert Future[Future[]] to Future[]
          val updateTask: Future[Seq[TaskGameInfo]] = aPromise.future.flatten
          // update game tasks from future result
          updateTask.map(x => task.copy(tasks = x))
        }
      }
    } yield (updatedTask)
  }
  // convernt Option[Future[T]] to Future[Option[T]]
  def f[A](v: Option[Future[A]]): Future[Option[A]] = v match {
     case Some(f) => f.map(Some(_))
     case None    => Future.successful(None)
  }

  // def getWeeklyTaskUpdates(user: String, gameID: UUID): Future[Option[DailyTask]]
  // def getTaskByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  // 	try {
  // 		for {
	 //      txs <- taskRepo.findByDateRange(
	 //      	start.getEpochSecond,
	 //      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	 //      	limit,
	 //      	offset)
	 //      size <- taskRepo.getSize()
	 //      hasNext <- Future(size - (offset + limit) > 0)
	 //    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  // 	} catch {
  // 		case e: Throwable => Future(Json.obj("err" -> e.toString))
  // 	}
  // }
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
 // def getTaskByMonthly(start: Instant,  end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
 //  	try {
 //  		for {
	//       txs <- taskRepo.findByDateRange(
	//       	start.getEpochSecond,
	//       	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	//       	limit,
	//       	offset)
	//       size <- taskRepo.getSize()
	//       hasNext <- Future(size - (offset + limit) > 0)
	//     } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
 //  	} catch {
 //  		case e: Throwable => Future(Json.obj("err" -> e.toString))
 //  	}
 //  }
 //  def getTaskBy(start: Instant, limit: Int, offset: Int): Future[JsValue] = {
 //    try {
 //      for {
 //        txs <- taskRepo.findBy(start.getEpochSecond, limit, offset)
 //        size <- taskRepo.getSize()
 //        hasNext <- Future(size - (offset + limit) > 0)
 //      } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
 //    } catch {
 //      case e: Throwable => Future(Json.obj("err" -> e.toString))
 //    }
 //  }
}