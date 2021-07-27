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

  def update(v: MahjongHiloHistory): Future[Int] =
    db.run(dao.Query.filter(_.gameID === v.gameID).update(v))

  def findByUserGameIDAndGameID(userGameID: Int, gameID: String): Future[Option[MahjongHiloHistory]] =
    db.run(dao.Query.filter(x => x.userGameID === userGameID && x.gameID === gameID).result.headOption)

  def all(): Future[Seq[MahjongHiloHistory]] =
    db.run(dao.Query.result)
}
