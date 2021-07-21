package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GhostQuestBattleResult

@Singleton
class GhostQuestBattleResultRepo @Inject()(
    dao: models.dao.GhostQuestBattleResultDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def insert(data: GhostQuestBattleResult): Future[Int] =
    db.run(dao.Query += data)

  def removeAll(): Future[Int] =
    db.run(dao.Query.clearTbl)

  def all(): Future[Seq[GhostQuestBattleResult]] =
    db.run(dao.Query.result)
}
