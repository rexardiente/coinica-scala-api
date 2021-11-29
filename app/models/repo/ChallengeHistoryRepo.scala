package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ ChallengeHistory }

@Singleton
class ChallengeHistoryRepo @Inject()(
    dao: models.dao.ChallengeHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def add(challengeHistory: ChallengeHistory): Future[Int] =
    db.run(dao.Query += challengeHistory)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(challenge: ChallengeHistory): Future[Int] =
    db.run(dao.Query.filter(_.id === challenge.id).update(challenge))

  def findByDate(date: Long): Future[Option[ChallengeHistory]] =
    db.run(dao.Query.filter(x => x.createdAt >= date).result.headOption)
}