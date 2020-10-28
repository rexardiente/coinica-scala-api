package models.repo.ranking

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ranking.Ranking

@Singleton
class RankingRepo @Inject()(
    dao: models.dao.ranking.RankingDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(ranking: Ranking): Future[Int] =
    db.run(dao.Query += ranking)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(ranking: Ranking): Future[Int] =
    db.run(dao.Query.filter(_.id === ranking.id).update(ranking))

  def all(): Future[Seq[Ranking]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Ranking]] =
    db.run(dao.Query.filter(r => r.id === id)
      .result
      .headOption)

  
}