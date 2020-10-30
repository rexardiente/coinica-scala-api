package models.repo.task

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.task.Task

@Singleton
class TaskRepo @Inject()(
    dao: models.dao.task.TaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(task: Task): Future[Int] =
    db.run(dao.Query += task)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(task: Task): Future[Int] =
    db.run(dao.Query.filter(_.id ===task.id).update(task))

  def all(): Future[Seq[Task]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .result
      .headOption)

  def findByDaily(id: UUID, currentdate: Instant): Future[Seq[Task]] =
    db.run(dao.Query.filter(r => r.id === id && r.date === currentdate ) 
      .result)

  def findByWeekly(id: UUID, startdate: Instant, enddate : Instant): Future[Seq[Task]] =
    db.run(dao.Query.filter(r => r.id === id && r.date >= startdate && r.date <= enddate ) 
      .result)
 
  def findBygameName(gameID: UUID): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.gameID === gameID)
      .result
      .headOption)
}
