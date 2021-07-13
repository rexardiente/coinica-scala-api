package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import java.time.Instant
import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.service.DynamicSortBySupport._
import models.domain.eosio._

@Singleton
class GhostQuestCharacterRepo @Inject()(
    dataDAO: models.dao.GhostQuestCharacterDAO,
    // dataHistoryDAO: models.dao.GhostQuestCharacterHistoryDAO,
    // gameHistoryDAO: models.dao.GQCharacterGameHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  // def insert(data: GhostQuestCharacter): Future[Int] =
  //   db.run(dataDAO.Query += data)

  // def remove(user: UUID, key: String): Future[Int] =
  //   db.run(dataDAO.Query.filter(x => x.owner === user && x.key === key).delete)

  // def update(data: GhostQuestCharacter): Future[Int] =
  //   db.run(dataDAO.Query.filter(x => x.owner === data.owner && x.key === data.key).update(data))

  // def all(): Future[Seq[GhostQuestCharacter]] =
  //   db.run(dataDAO.Query.result)

  // def exist(key: String): Future[Boolean] =
  //   db.run(dataDAO.Query(key).exists.result)

  // def find(id: String): Future[Option[GhostQuestCharacter]] =
  //   db.run(dataDAO.Query(id).result.headOption)

}
