package models.repo.dailychallenge

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.dailychallenge.Dailychallenge

@Singleton
class DailychallengeRepo @Inject()(
    dao: models.dao.dailychallenge.DailychallengeDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(dailychallenge: Dailychallenge): Future[Int] =
    db.run(dao.Query += dailychallenge)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(dailychallenge: Dailychallenge): Future[Int] =
    db.run(dao.Query.filter(_.id === dailychallenge.id).update(dailychallenge))

  def all(): Future[Seq[Dailychallenge]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Dailychallenge]] =
    db.run(dao.Query.filter(r => r.id === id)
      .result
      .headOption)

  def findBygameName(gamename: String): Future[Option[Dailychallenge]] =
    db.run(dao.Query.filter(r => r.gamename === gamename)
      .result
      .headOption)
}
