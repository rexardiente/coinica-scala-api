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

  def exist(id: String): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  def filteredByID(id: String): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query(id).result)
  // check if user has history by using winner and loser field ID
  def getByUser(player: UUID): Future[Seq[GQCharacterGameHistory]] = {
    for {
      aswinner <- db.run(dao.Query.filter(v => v.winner === player).result)
      asloser <- db.run(dao.Query.filter(v => v.loser === player).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }
  }

  def getByUsernameAndCharacterID(player: UUID, id: String): Future[Seq[GQCharacterGameHistory]] =
    for {
      aswinner <- db.run(dao.Query.filter(v => v.winnerID === id && v.winner === player).result)
      asloser <- db.run(dao.Query.filter(v => v.loserID === id && v.loser === player).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }

  def getByUsernameAndGameID(id: String, player: UUID): Future[Seq[GQCharacterGameHistory]] =
    for {
      filtered <- filteredByID(id)
      aswinner <- Future.successful(filtered.filter(_.winner == player))
      asloser <- Future.successful(filtered.filter(_.loser == player))
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }

  def getByCharacterID(id: String): Future[Seq[GQCharacterGameHistory]] =
    for {
      aswinner <- db.run(dao.Query.filter(_.winnerID === id).result)
      asloser <- db.run(dao.Query.filter(_.loserID === id).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }
}
