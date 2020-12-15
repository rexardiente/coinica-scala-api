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
  //   db.run(dao.Query.filter(x => x.owner === data.owner && x.game_id === data.game_id).update(data))

  def all(): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query.result)

  def exist(id: String): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  // def find(id: UUID): Future[Option[GQCharacterGameHistory]] =
  //   db.run(dao.Query(id).result.headOption)

  // def find(user: String, key: Long): Future[Boolean] = 
  //   db.run(dao.Query.filter(x => x.owner === user && x.key === key).exists.result)
}
