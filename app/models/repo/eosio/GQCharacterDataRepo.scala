package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQCharacterData

@Singleton
class GQCharacterDataRepo @Inject()(
    dao: models.dao.GQCharacterDataDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def insert(data: GQCharacterData): Future[Int] =
    db.run(dao.Query += data)

  def update(data: GQCharacterData): Future[Int] =
    db.run(dao.Query.filter(x => x.owner === data.owner && x.chracterID === data.chracterID).update(data))

  def all(): Future[Seq[GQCharacterData]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = 
    db.run(dao.Query(id).exists.result)

  def find(id: UUID): Future[Option[GQCharacterData]] =
    db.run(dao.Query(id).result.headOption)

  def find(user: String, chracterID: Long): Future[Boolean] = 
    db.run(dao.Query.filter(x => x.owner === user && x.chracterID === chracterID).exists.result)
}
