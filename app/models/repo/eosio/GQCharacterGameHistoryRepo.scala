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

  // def exist(id: String, player: String): Future[Boolean] =
  //   db.run(dao.Query(id, player).exists.result)

  def exist(id: String): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  // def existByID(id: String, characterID: String): Future[Boolean] =
  //   db.run(dao.Query.filter(x => x.id === id && x.playerID === characterID).exists.result)

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  def filteredByID(id: String): Future[Seq[GQCharacterGameHistory]] =
    db.run(dao.Query(id).result)
  // check if user has history by using player1 and player2 field ID
  def getByUser(player: String): Future[Seq[GQCharacterGameHistory]] = {
    for {
      asPlayer1 <- db.run(dao.Query.filter(v => v.player1 === player).result)
      asPlayer2 <- db.run(dao.Query.filter(v => v.player2 === player).result)
    } yield (asPlayer1, asPlayer2) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }
  }

  // def getByIDs(player: String, enemy: String): Future[Seq[GQCharacterGameHistory]] =
  //   db.run(dao.Query.filter(x => x.playerID === player && x.enemyID === enemy).result)

  def getByUsernameAndCharacterID(id: String, player: String): Future[Seq[GQCharacterGameHistory]] =
    for {
      asPlayer1 <- db.run(dao.Query.filter(v => v.player1ID === id && v.player1 === player).result)
      asPlayer2 <- db.run(dao.Query.filter(v => v.player2ID === id && v.player2 === player).result)
    } yield (asPlayer1, asPlayer2) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }

  def getByUsernameAndGameID(id: String, player: String): Future[Seq[GQCharacterGameHistory]] =
    for {
      filtered <- filteredByID(id)
      asPlayer1 <- Future.successful(filtered.filter(_.player1 == player))
      asPlayer2 <- Future.successful(filtered.filter(_.player2 == player))
    } yield (asPlayer1, asPlayer2) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }

  // def findByID(id: String): Future[Option[GQCharacterGameHistory]] =
  //   db.run(dao.Query(id).result.headOption)

  def getByCharacterID(id: String): Future[Seq[GQCharacterGameHistory]] =
    for {
      asPlayer1 <- db.run(dao.Query.filter(_.player1ID === id).result)
      asPlayer2 <- db.run(dao.Query.filter(_.player2ID === id).result)
    } yield (asPlayer1, asPlayer2) match {
      case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
    }

  // def getTotalHistory(id: String, player: String): Future[Int] =
  //   db.run(dao.Query.filter(x => x.playerID === id && x.player === player).size.result)
}
