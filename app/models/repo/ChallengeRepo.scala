package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Challenge

@Singleton
class ChallengeRepo @Inject()(
    dao: models.dao.ChallengeDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  private val createAtFn = SimpleFunction.unary[Instant, Long]("CREATED_AT")

  def add(challenge: Challenge): Future[Int] =
    db.run(dao.Query += challenge)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(challenge: Challenge): Future[Int] =
    db.run(dao.Query.filter(_.id === challenge.id).update(challenge))

  def existByDate(createAt: Instant): Future[Boolean] =
    db.run(dao.Query(createAt).exists.result)

  def all(): Future[Seq[Challenge]] =
    db.run(dao.Query.result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)
}