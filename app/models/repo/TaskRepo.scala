package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
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
    db.run(dao.Query.filter(_.id ===task.id).update(task))

  def all(limit: Int, offset: Int): Future[Seq[Task]] =
    db.run(dao.Query
        .drop(offset)
      .take(limit)
       .result)

  def exist(id: UUID): Future[Boolean] = 
    db.run(dao.Query(id).exists.result)

  def findByID(id: UUID, limit: Int, offset: Int): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .drop(offset)
      .take(limit)
      .result
      .headOption)

  def findByDaily(id: UUID, currentdate: Long): Future[Seq[Task]] = 
   db.run(dao.Query.filter(r => r.id === id && r.datecreated === currentdate) 
      .result)

  def findByWeekly(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Option[Task]] =
   db.run(dao.Query.filter(r => r.datecreated >= startdate && r.datecreated <= enddate ) 
     .drop(offset)
      .take(limit)
      .result
       .headOption)

  def findAll(limit: Int, offset: Int): Future[Seq[Task]] = 
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =  
    db.run(dao.Query.length.result)

  def findBygameName(gameID: UUID): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.gameID === gameID)
      .result
      .headOption)
}
/*
   def getByDate(start: Long, end: Long, limit: Int, offset: Int): Future[Seq[Task]] = {
    db.run(dao.Query
      .filter(r => r.blockTimestamp >= start && r.blockTimestamp <= end)
      .drop(offset)
      .take(limit)
    .result)
  }
  */
