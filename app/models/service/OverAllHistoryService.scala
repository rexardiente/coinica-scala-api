package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain._
import models.repo._
import utils.SystemConfig.{ instantNowUTC, dateNowPlusDaysUTC }

@Singleton
class OverAllHistoryService @Inject()(
    userAccountRepo: UserAccountRepo,
    gameRepo: GameRepo,
    overallGameHistory: OverAllGameHistoryRepo) {
  // def paginatedResult[T >: ReferralHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
  //    for {
  //     tasks <- referralRepo.findAll(limit, offset)
  //     size <- referralRepo.getSize()
  //     hasNext <- Future(size - (offset + limit) > 0)
  //   } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  // }
  def getOverallGameHistoryByGameIDs(seq: Seq[String]): Future[Seq[OverAllGameHistory]] = {
    for {
      histories <- Future.sequence(seq.map(getOverallGameHistoryByGameID(_)))
      // remove none results
      process <- Future.successful(histories.flatten)
    } yield(process)
  }
  def getOverallGameHistoryByGameID(id: String): Future[Option[OverAllGameHistory]] =
    overallGameHistory.getByGameID(id)

  def all(limit: Int): Future[Seq[OverAllGameHistory]] = overallGameHistory.all(limit)
  // get latest 10 result for history..
  def gameHistoryByGameID(id: UUID): Future[Seq[OverAllGameHistory]] = {
    for {
      hasGame <- gameRepo.findByID(id)
      txs <- {
        hasGame
          .map { game =>
            val start: Long = dateNowPlusDaysUTC(-7).getEpochSecond
            val end: Long = instantNowUTC().getEpochSecond
            // fetch only range of 1 week of txs
            overallGameHistory
              .getByDateRangeAndGame(game.name, start, end)
              .map(x => x.sortBy(- _.createdAt).take(10))
          }
          .getOrElse(Future.successful(Seq.empty))
      }
    } yield (txs)
  }

  def gameHistoryByGameIDAndUser(username: String, gameID: UUID): Future[Seq[OverAllGameHistory]] = {
    for {
      hasGame <- gameRepo.findByID(gameID)
      txs <- {
        hasGame
          .map { game =>
            overallGameHistory
              .getByGameName(game.name)
              .map(_.filter(_.info.user == username))
              .map(_.sortBy(- _.createdAt).take(10))
          }
          .getOrElse(Future.successful(Seq.empty))
      }
    } yield (txs)
  }
  def gameIsExistsByTxHash(txHash: String): Future[Boolean] = overallGameHistory.isExistsByTxHash(txHash)
  def addHistory(history: OverAllGameHistory): Future[Int] = overallGameHistory.add(history)
}