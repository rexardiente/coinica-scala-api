// package models.repo.eosio

// import javax.inject.{ Inject, Singleton }
// import java.util.UUID
// import java.time.Instant
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.Future
// import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
// import models.domain.eosio.GQCharacterGameHistory

// @Singleton
// class GQCharacterGameHistoryRepo @Inject()(
//     gameHistoryDAO: models.dao.GQCharacterGameHistoryDAO,
//     protected val dbConfigProvider: DatabaseConfigProvider
//   ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
//   import profile.api._

//   private def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)
//   def getGameHistoryByDateRange(from: Long, to: Long): Future[Seq[GQCharacterGameHistory]] =
//     db.run(gameHistoryDAO.Query.filter(x => x.timeExecuted >= from && x.timeExecuted <= to).result)

//   def getAllGameHistory(): Future[Seq[GQCharacterGameHistory]] =
//     db.run(gameHistoryDAO.Query.take(10).result)

//   def insertGameHistory(data: GQCharacterGameHistory): Future[Int] =
//     db.run(gameHistoryDAO.Query += data)

//   def allGameHistory(): Future[Seq[GQCharacterGameHistory]] =
//     db.run(gameHistoryDAO.Query.result)

//   def existGameHistory(id: String): Future[Boolean] =
//     db.run(gameHistoryDAO.Query(id).exists.result)

//   def filteredGameHistoryByID(id: String): Future[Seq[GQCharacterGameHistory]] =
//     db.run(gameHistoryDAO.Query(id).result)
//   // check if user has history by using winner and loser field ID
//   def getGameHistoryByUserID(player: UUID): Future[Seq[GQCharacterGameHistory]] = {
//     for {
//       aswinner <- db.run(gameHistoryDAO.Query.filter(v => v.winner === player).result)
//       asloser <- db.run(gameHistoryDAO.Query.filter(v => v.loser === player).result)
//     } yield (aswinner, asloser) match {
//       case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
//     }
//   }
//   // add date range by month of Txs
//   def getGameHistoryByUsernameCharacterIDAndDate(player: UUID, id: String, startDate: Long, endDate: Long):
//     Future[Seq[GQCharacterGameHistory]] =
//       for {
//         aswinner <- db.run(gameHistoryDAO.Query.filter { v =>
//             v.winnerID === id && v.winner === player && v.timeExecuted >= startDate && v.timeExecuted <= endDate
//           }.result)
//         asloser <- db.run(gameHistoryDAO.Query.filter { v =>
//             v.loserID === id && v.loser === player && v.timeExecuted >= startDate && v.timeExecuted <= endDate
//           }.result)
//       } yield (aswinner, asloser) match {
//         case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
//       }

//   def getGameHistoryByUsernameAndCharacterID(player: UUID, id: String): Future[Seq[GQCharacterGameHistory]] =
//     for {
//       aswinner <- db.run(gameHistoryDAO.Query.filter(v => v.winnerID === id && v.winner === player).result)
//       asloser <- db.run(gameHistoryDAO.Query.filter(v => v.loserID === id && v.loser === player).result)
//     } yield (aswinner, asloser) match {
//       case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
//     }

//   def getGameHistoryByUsernameAndGameID(id: String, player: UUID): Future[Seq[GQCharacterGameHistory]] =
//     for {
//       filtered <- filteredGameHistoryByID(id)
//       aswinner <- Future.successful(filtered.filter(_.winner == player))
//       asloser <- Future.successful(filtered.filter(_.loser == player))
//     } yield (aswinner, asloser) match {
//       case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
//     }

//   def getGameHistoryByCharacterID(id: String): Future[Seq[GQCharacterGameHistory]] =
//     for {
//       aswinner <- db.run(gameHistoryDAO.Query.filter(_.winnerID === id).result)
//       asloser <- db.run(gameHistoryDAO.Query.filter(_.loserID === id).result)
//     } yield (aswinner, asloser) match {
//       case (p1, p2) => mergeSeq[GQCharacterGameHistory, Seq[GQCharacterGameHistory]](p1, p2)
//     }
// }
