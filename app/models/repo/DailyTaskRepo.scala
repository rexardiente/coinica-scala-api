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
    dao: models.dao.TaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(task: DailyTask): Future[Int] =
    db.run(dao.DailyTaskQuery += task)

  def delete(user: UUID): Future[Int] =
    db.run(dao.DailyTaskQuery(user).delete)

  def clearTable(): Future[Int] =
    db.run(dao.DailyTaskQuery.clearTbl)

  def update(task: DailyTask): Future[Int] =
    db.run(dao.DailyTaskQuery.filter(x => x.user === task.user && x.gameID === task.game_id).update(task))

  def addOrUpdate(task: DailyTask): Future[Int] = {
    for {
      find <- getTodayTaskByUserAndGame(task.user, task.game_id)
      check <- {
        find match {
          // if found auto add 1 on its game count
          case Some(v) => update(v.copy(game_count = (v.game_count + task.game_count)))
          // else add to DB
          case _ => add(task)
        }
      }
    } yield (check)
  }

  def findUserByID(user: UUID): Future[Option[DailyTask]] =
    db.run(dao.DailyTaskQuery.filter(x => x.user === user).result.headOption)

  def all(): Future[Seq[DailyTask]] =
    db.run(dao.DailyTaskQuery.result)

  def getTodayTaskByUserAndGame(user: UUID, gameID: UUID): Future[Option[DailyTask]] =
    db.run(dao.DailyTaskQuery.filter(x => x.user === user && x.gameID === gameID).result.headOption)

  def exist(user: UUID): Future[Boolean] =
    db.run(dao.DailyTaskQuery(user).exists.result)

  def exist(user: UUID, gameID: UUID): Future[Boolean] =
    db.run(dao.DailyTaskQuery(user, gameID).exists.result)
}