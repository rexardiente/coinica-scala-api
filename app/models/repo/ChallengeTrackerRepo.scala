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

  def delete(user: String): Future[Int] =
    db.run(dao.Query(user).delete)

  def clearTable(): Future[Int] =
    db.run(dao.Query.clearTbl)

  def update(challengeTracker: ChallengeTracker): Future[Int] =
    db.run(dao.Query.filter(_.user === challengeTracker.user).update(challengeTracker))

  def exist(user: String): Future[Boolean] =
    db.run(dao.Query(user).exists.result)

  def all(): Future[Seq[ChallengeTracker]] =
    db.run(dao.Query.result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)
}