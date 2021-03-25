package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.TaskHistory

@Singleton
class TaskHistoryRepo @Inject()(
    dao: models.dao.TaskHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(task: TaskHistory): Future[Int] =
    db.run(dao.Query += task)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(task: TaskHistory): Future[Int] =
    db.run(dao.Query.filter(_.id === task.id).update(task))

  def all(): Future[Seq[TaskHistory]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def existByDate(createdAt: Instant): Future[Boolean] =
    db.run(dao.Query.filter(_.createdAt === createdAt).exists.result)

  def findByID(id: UUID): Future[Seq[TaskHistory]] =
    db.run(dao.Query.filter(r => r.id === id).result)
  // def findByDateRange(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Seq[TaskHistory]] =
  //  db.run(dao.Query.filter(r => r.datecreated >= startdate && r.datecreated <= enddate )
  //    .drop(offset)
  //    .take(limit)
  //    .result)
  // def formatDateFromlong(date: Long): String = {
  //   val d = new Date(date * 1000L)
  //   new SimpleDateFormat("yyyy-MM-d").format(d)
  // }

  def withLimit(limit: Int, offset: Int): Future[Seq[TaskHistory]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)

  def getMonthlyTaskByUserAndGame(user: UUID, gameID: UUID, start: Instant, end: Instant): Future[Seq[TaskHistory]] =
    db.run(dao.Query.filter(x => x.user === user && x.gameID === gameID && x.createdAt >= start && x.expiredAt >= end).result)
}



