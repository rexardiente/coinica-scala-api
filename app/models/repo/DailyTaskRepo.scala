package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.DailyTask

@Singleton
class DailyTaskRepo @Inject()(
    dao: models.dao.DailyTaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(task: DailyTask): Future[Int] =
    db.run(dao.Query += task)

  def delete(user: UUID): Future[Int] =
    db.run(dao.Query(user).delete)

  def clearTable(): Future[Int] =
    db.run(dao.Query.clearTbl)

  def update(task: DailyTask): Future[Int] =
    db.run(dao.Query.filter(x => x.user === task.user && x.gameID === task.game_id).update(task))

  def all(): Future[Seq[DailyTask]] =
    db.run(dao.Query.result)

  def getTodayTaskByUserAndGame(user: UUID, gameID: UUID): Future[Option[DailyTask]] =
    db.run(dao.Query.filter(x => x.user === user && x.gameID === gameID).result.headOption)

  def exist(user: UUID): Future[Boolean] =
    db.run(dao.Query(user).exists.result)

  def exist(user: UUID, gameID: UUID): Future[Boolean] =
    db.run(dao.Query(user, gameID).exists.result)
}