package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Task

@Singleton
class TaskRepo @Inject()(
    dao: models.dao.TaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(task: Task): Future[Int] =
    db.run(dao.Query += task)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(task: Task): Future[Int] =
    db.run(dao.Query.filter(_.id === task.id).update(task))

  def all(): Future[Seq[Task]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def existByDate(createdAt: Instant): Future[Boolean] =
    db.run(dao.Query.filter(_.createdAt === createdAt).exists.result)

  def findByID(id: UUID): Future[Seq[Task]] =
    db.run(dao.Query.filter(r => r.id === id).result)

  def getDailyTaskByDate(createdAt: Instant): Future[Option[Task]] =
    db.run(dao.Query.filter(_.createdAt === createdAt).result.headOption)
  // def findByDateRange(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Seq[Task]] =
  //  db.run(dao.Query.filter(r => r.datecreated >= startdate && r.datecreated <= enddate )
  //    .drop(offset)
  //    .take(limit)
  //    .result)
  // def formatDateFromlong(date: Long): String = {
  //   val d = new Date(date * 1000L)
  //   new SimpleDateFormat("yyyy-MM-d").format(d)
  // }

  def withLimit(limit: Int, offset: Int): Future[Seq[Task]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)
}