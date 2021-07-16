package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GhostQuestCharacterGameHistory

@Singleton
class GhostQuestCharacterGameHistoryRepo @Inject()(
    dao: models.dao.GhostQuestCharacterGameHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  private def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)
  def getGameHistoryByDateRange(from: Long, to: Long): Future[Seq[GhostQuestCharacterGameHistory]] =
    db.run(dao.Query.filter(x => x.timeExecuted >= from && x.timeExecuted <= to).result)

  def getAllGameHistory(): Future[Seq[GhostQuestCharacterGameHistory]] =
    db.run(dao.Query.take(10).result)

  def insertGameHistory(data: GhostQuestCharacterGameHistory): Future[Int] =
    db.run(dao.Query += data)

  def allGameHistory(): Future[Seq[GhostQuestCharacterGameHistory]] =
    db.run(dao.Query.result)

  def existGameHistory(id: String): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def filteredGameHistoryByID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    db.run(dao.Query(id).result)
  // check if user has history by using winner and loser field ID
  def getGameHistoryByUserID(ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] = {
    for {
      aswinner <- db.run(dao.Query.filter(v => v.winner === ownerID).result)
      asloser <- db.run(dao.Query.filter(v => v.loser === ownerID).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GhostQuestCharacterGameHistory, Seq[GhostQuestCharacterGameHistory]](p1, p2)
    }
  }
  // add date range by month of Txs
  def getGameHistoryByUsernameCharacterIDAndDate(id: String, ownerID: Int, startDate: Long, endDate: Long):
    Future[Seq[GhostQuestCharacterGameHistory]] =
      for {
        aswinner <- db.run(dao.Query.filter { v =>
            v.winnerID === id && v.winner === ownerID && v.timeExecuted >= startDate && v.timeExecuted <= endDate
          }.result)
        asloser <- db.run(dao.Query.filter { v =>
            v.loserID === id && v.loser === ownerID && v.timeExecuted >= startDate && v.timeExecuted <= endDate
          }.result)
      } yield (aswinner, asloser) match {
        case (p1, p2) => mergeSeq[GhostQuestCharacterGameHistory, Seq[GhostQuestCharacterGameHistory]](p1, p2)
      }

  def getGameHistoryByGameIDAndCharacterID(id: String, ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
    for {
      aswinner <- db.run(dao.Query.filter(v => v.winnerID === id && v.winner === ownerID).result)
      asloser <- db.run(dao.Query.filter(v => v.loserID === id && v.loser === ownerID).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GhostQuestCharacterGameHistory, Seq[GhostQuestCharacterGameHistory]](p1, p2)
    }

  // def getGameHistoryByUsernameAndGameID(id: String, ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
  //   for {
  //     filtered <- filteredGameHistoryByID(id)
  //     aswinner <- Future.successful(filtered.filter(_.winner == ownerID))
  //     asloser <- Future.successful(filtered.filter(_.loser == ownerID))
  //   } yield (aswinner, asloser) match {
  //     case (p1, p2) => mergeSeq[GhostQuestCharacterGameHistory, Seq[GhostQuestCharacterGameHistory]](p1, p2)
  //   }

  def getGameHistoryByCharacterID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    for {
      aswinner <- db.run(dao.Query.filter(_.winnerID === id).result)
      asloser <- db.run(dao.Query.filter(_.loserID === id).result)
    } yield (aswinner, asloser) match {
      case (p1, p2) => mergeSeq[GhostQuestCharacterGameHistory, Seq[GhostQuestCharacterGameHistory]](p1, p2)
    }
}
