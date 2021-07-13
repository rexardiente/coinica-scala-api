package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GhostQuestCharacter

@Singleton
class GhostQuestCharacterRepo @Inject()(
    dao: models.dao.GhostQuestCharacterDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def insert(data: GhostQuestCharacter): Future[Int] =
    db.run(dao.Query += data)

  def removeAll(): Future[Int] =
    db.run(dao.Query.clearTbl)

  def update(data: GhostQuestCharacter): Future[Int] =
    db.run(dao.Query.filter(_.key === data.key).update(data))

  def all(): Future[Seq[GhostQuestCharacter]] =
    db.run(dao.Query.result)

  def exist(key: String): Future[Boolean] =
    db.run(dao.Query(key).exists.result)

  def find(key: String): Future[Option[GhostQuestCharacter]] =
    db.run(dao.Query(key).result.headOption)
}
