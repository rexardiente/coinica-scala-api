package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQCharacterGameHistory

@Singleton
class GQCharacterGameHistoryRepo @Inject()(
    dao: models.dao.GQCharacterGameHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def insert(data: GQCharacterGameHistory): Future[Int] =
    db.run(dao.Query += data)

  def all(): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query.result)

  def exist(id: String, player: String): Future[Boolean] =
    db.run(dao.Query(id, player).exists.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def existByID(gameID: String, characterID: String): Future[Boolean] =
    db.run(dao.Query.filter(x => x.gameID === gameID && x.playerID === characterID).exists.result)

  def getByPlayerName(player: String): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query.filter(_.player === player).result)

  def getByIDs(player: String, enemy: String): Future[Seq[GQCharacterGameHistory]] = 
    db.run(dao.Query.filter(x => x.playerID === player && x.enemyID === enemy).result)

  def find(id: String, player: String): Future[Option[GQCharacterGameHistory]] =
    db.run(dao.Query.filter(x => x.playerID === id && x.player === player).result.headOption)

  def totalByIDs(id: String, player: String): Future[Int] =
    db.run(dao.Query.filter(x => x.playerID === id && x.player === player).size.result)
}
