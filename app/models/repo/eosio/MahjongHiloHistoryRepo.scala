package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.MahjongHiloHistory

@Singleton
class MahjongHiloHistoryRepo @Inject()(
    dao: models.dao.MahjongHiloHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def insert(data: MahjongHiloHistory): Future[Int] =
    db.run(dao.Query += data)

  def delete(gameID: String): Future[Int] =
    db.run(dao.Query(gameID).delete)

  def update(v: MahjongHiloHistory): Future[Int] =
    db.run(dao.Query.filter(_.gameID === v.gameID).update(v))

  def findByUserGameIDAndGameID(gameID: String, userGameID: Int): Future[Option[MahjongHiloHistory]] =
    db.run(dao.Query.filter(x => x.userGameID === userGameID && x.gameID === gameID && x.status === false).result.headOption)

  def getByUserGameID(userGameID: Int, limit: Int): Future[Seq[MahjongHiloHistory]] = {
    for {
      query <- db.run(dao.Query.filter(_.userGameID === userGameID).sortBy(_.createdAt.desc).take(limit).result)
      // remove empty result predictionsfrom list
      filtered <- Future.successful(query.filter(!_.predictions.isEmpty))
    } yield (filtered)
  }

  def getByUserGameID(userGameID: Int): Future[Seq[MahjongHiloHistory]] =
    db.run(dao.Query.filter(_.userGameID === userGameID).sortBy(_.createdAt.desc).result)

  def all(): Future[Seq[MahjongHiloHistory]] =
    db.run(dao.Query.result)
}
