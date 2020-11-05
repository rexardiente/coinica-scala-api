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

  def all(limit: Int, offset: Int): Future[Seq[Task]] =
    db.run(dao.Query
        .drop(offset)
      .take(limit)
       .result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)


  def findByID(id: UUID, limit: Int, offset: Int): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .drop(offset)
      .take(limit)
      .result
      .headOption)


  def findByDaily(id: UUID, currentdate: Instant): Future[Seq[Task]] =
    db.run(dao.Query.filter(r => r.id === id && r.datecreated === currentdate ) 
      .result)

  def findByWeekly(id: UUID, startdate: Instant, enddate : Instant): Future[Seq[Task]] =
    db.run(dao.Query.filter(r => r.id === id && r.datecreated >= startdate && r.datecreated <= enddate ) 
      .result)
 
  def findBygameName(gameID: UUID): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.gameID === gameID)
      .result
      .headOption)
      
}
