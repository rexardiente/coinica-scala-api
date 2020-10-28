package models.repo.dailytask

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.dailytask.Dailytask

@Singleton
class DailytaskRepo @Inject()(
    dao: models.dao.dailytask.DailytaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(dailytask: Dailytask): Future[Int] =
    db.run(dao.Query += dailytask)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(dailytask: Dailytask): Future[Int] =
    db.run(dao.Query.filter(_.id === dailytask.id).update(dailytask))

  def all(): Future[Seq[Dailytask]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Dailytask]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .result
      .headOption)
   def findByWeekly(id: UUID, startdate: Instant, enddate : Instant): Future[Seq[Dailytask]] =
       db.run(dao.Query.filter(r => r.id === id && r.taskdate >= startdate && r.taskdate <= enddate ) 
      .result)
 
  def findBygameName(gamename: String): Future[Option[Dailytask]] =
    db.run(dao.Query.filter(r => r.gamename === gamename)
      .result
      .headOption)
}
