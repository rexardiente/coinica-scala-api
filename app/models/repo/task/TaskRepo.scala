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
/*
 def findAll(userId: Long, limit: Int, offset: Int) = db.run{
  query.filter(_.creatorId === userId).drop(offset).take(limit).result
  def findAll(userId: Long, limit: Int, offset: Int) = db.run {
  for {
    comments <- query.filter(_.creatorId === userId)
                     .drop(offset).take(limit)
                     .result
    numberOfComments <- query.filter(_.creatorId === userId).length.result
  } yield PaginatedResult(
    totalCount = numberOfComments,
    entities = comments.toList,
    hasNextPage = numberOfComments - (offset + limit) > 0
  )
}
} 
  */

  def findByID(id: UUID, limit: Int, offset: Int): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .drop(offset)
      .take(limit)
      .result
      .headOption)

/*
def findAll(id: UUID, limit: Int, offset: Int) = db.run {
  for {
   task <- dao.Query.filter(r => r.id === id)
                     .drop(offset).take(limit)
                     .result
    numberOfComments <- dao.Query.filter(r => r.id === id).length.result
  } yield PaginatedResult(
    totalCount = numberOfComments,
    entities = comments.toList,
    hasNextPage = numberOfComments - (offset + limit) > 0
  )
}
*/
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
