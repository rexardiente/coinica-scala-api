package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GhostQuestCharacterHistory

@Singleton
class GhostQuestCharacterHistoryRepo @Inject()(
    dao: models.dao.GhostQuestCharacterHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  def insert(data: GhostQuestCharacterHistory): Future[Int] =
    db.run(dao.Query += data)

  def remove(id: Int, key: String): Future[Int] =
    db.run(dao.Query.filter(x => x.owner_id === id && x.key === key).delete)

  def update(data: GhostQuestCharacterHistory): Future[Int] =
    db.run(dao.Query.filter(x => x.owner_id === data.owner_id && x.key === data.key).update(data))

  def all(): Future[Seq[GhostQuestCharacterHistory]] =
    db.run(dao.Query.result)

  def exist(key: String): Future[Boolean] =
    db.run(dao.Query(key).exists.result)

  def exist(key: String, owner_id: Int): Future[Boolean] =
    db.run(dao.Query(key, owner_id).exists.result)

  def find(key: String): Future[Option[GhostQuestCharacterHistory]] =
    db.run(dao.Query(key).result.headOption)

  def findByOwnerID(id: Int): Future[Seq[GhostQuestCharacterHistory]] =
    db.run(dao.Query.filter(_.owner_id === id).result)

  def findByOwnerIDAndID(id: Int, key: String): Future[Seq[GhostQuestCharacterHistory]] =
    db.run(dao.Query.filter(x => x.owner_id === id && x.key === key).result)
}
