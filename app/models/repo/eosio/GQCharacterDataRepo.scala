package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ GQCharacterData, GQCharacterDataHistory }

@Singleton
class GQCharacterDataRepo @Inject()(
    dataDAO: models.dao.GQCharacterDataDAO,
    dataHistoryDAO: models.dao.GQCharacterDataHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  def insert(data: GQCharacterData): Future[Int] =
    db.run(dataDAO.Query += data)

  def remove(user: String, id: String): Future[Int] =
    db.run(dataDAO.Query.filter(x => x.owner === user && x.id === id).delete)

  def update(data: GQCharacterData): Future[Int] =
    db.run(dataDAO.Query.filter(x => x.owner === data.owner && x.id === data.id).update(data))

  def all(): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.result)

  def exist(id: String): Future[Boolean] =
    db.run(dataDAO.Query(id).exists.result)

  def find(id: String): Future[Option[GQCharacterData]] =
    db.run(dataDAO.Query(id).result.headOption)

  def find(user: String, id: String): Future[Boolean] =
    db.run(dataDAO.Query.filter(x => x.owner === user && x.id === id).exists.result)

  def getByUserAndID(user: String, id: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(x => x.owner === user && x.id === id).result)

  def getByID(id: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.id === id).result)

  def getByUser(user: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.owner === user).result)

  def getNoLifeCharacters(): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.life < 1).result)

  // History Functions...
  def insertHistory(data: GQCharacterDataHistory): Future[Int] =
    db.run(dataHistoryDAO.Query += data)

  def getHistoryByUser(user: String): Future[Seq[GQCharacterDataHistory]] =
    db.run(dataHistoryDAO.Query.filter(_.player === user).result)

  def getCharacterHistoryByUserAndID(user: String, id: String): Future[Seq[GQCharacterDataHistory]] =
    db.run(dataHistoryDAO.Query.filter(v => v.player === user && v.id === id).result)
}
