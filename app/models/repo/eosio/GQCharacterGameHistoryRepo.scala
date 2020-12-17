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

  // def update(data: GQCharacterGameHistory): Future[Int] =
  //   db.run(dao.Query.filter(x => x.player === data.player && x.game_id === data.game_id).update(data))

  def all(): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query.result)

  def exist(id: String, player: String): Future[Boolean] =
    db.run(dao.Query(id, player).exists.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def find(id: String, player: String): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query.filter(x => x.playerID === id && x.player === player).result)

  def getSize(id: String, player: String): Future[Int] =
    db.run(dao.Query.filter(x => x.playerID === id && x.player === player).size.result)
}
