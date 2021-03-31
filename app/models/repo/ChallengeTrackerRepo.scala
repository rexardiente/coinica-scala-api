package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ChallengeTracker

@Singleton
class ChallengeTrackerRepo @Inject()(
    dao: models.dao.ChallengeTrackerDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(challengeTracker: ChallengeTracker): Future[Int] =
    db.run(dao.Query += challengeTracker)

  def delete(user: UUID): Future[Int] =
    db.run(dao.Query(user).delete)

  def clearTable(): Future[Int] =
    db.run(dao.Query.clearTbl)

  def update(challengeTracker: ChallengeTracker): Future[Int] =
    db.run(dao.Query.filter(_.user === challengeTracker.user).update(challengeTracker))

  def exist(user: UUID): Future[Boolean] =
    db.run(dao.Query(user).exists.result)

  def all(): Future[Seq[ChallengeTracker]] =
    db.run(dao.Query.result)

  def addOrUpdate(task: ChallengeTracker): Future[Int] = {
    for {
      find <- findUserByID(task.user)
      check <- {
        find match {
          // user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double
          // if found auto add 1 on its game count
          case Some(v) =>
            val updatedChallenge: ChallengeTracker = v.copy(bets=(v.bets +  task.bets),
                                                            wagered=(v.wagered +  task.wagered),
                                                            ratio=(v.ratio +  task.ratio),
                                                            points=(v.points +  task.points))

            update(updatedChallenge)
          // else add to DB
          case _ => add(task)
        }
      }
    } yield (check)
  }

  def findUserByID(user: UUID): Future[Option[ChallengeTracker]] =
    db.run(dao.Query(user).result.headOption)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)
}