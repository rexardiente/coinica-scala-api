package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain._
import models.repo._

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
  def all(limit: Int): Future[Seq[OverAllGameHistory]] = overallGameHistory.all(limit)
  // get latest 10 result for history..
  def gameHistoryByGameID(id: UUID): Future[Seq[OverAllGameHistory]] = {
    for {
      game <- gameRepo.findByID(id)
      txs <- {
        if (game != None) {
          val now = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
          val lastSevenDays: Instant = now.plusDays(-7).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
          // fetch only range of 1 week of txs
          overallGameHistory
            .getByDateRangeAndGame(
                game.get.name.replaceAll(" ", "").toLowerCase,
                lastSevenDays.getEpochSecond,
                now.toInstant(ZoneOffset.UTC).getEpochSecond)
            .map(x => x.sortBy(- _.createdAt).take(10))
        }
        else Future(Seq.empty)
      }
    } yield (txs)
  }

  def gameHistoryByGameIDAndUser(account: UUID, gameID: UUID): Future[Seq[OverAllGameHistory]] = {
    for {
      game <- gameRepo.findByID(gameID)
      user <- userAccountRepo.getByID(account)
      txs <- {
        if (game != None && user != None) {
          // fetch only range of 1 month of txs
          val now = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
          val lastSevenDays: Instant = now.plusDays(-30).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
          // fetch only range of 1 week of txs
          overallGameHistory
            .getByDateRangeAndGame(
                game.get.name.replaceAll(" ", "").toLowerCase,
                lastSevenDays.getEpochSecond,
                now.toInstant(ZoneOffset.UTC).getEpochSecond)
            .map(_.filter(_.info.user == user.get.username))
            .map(_.sortBy(- _.createdAt).take(10))
        }
        else Future(Seq.empty)
      }
    } yield (txs)
  }
  def gameIsExistsByTxHash(txHash: String): Future[Boolean] = overallGameHistory.isExistsByTxHash(txHash)
  def gameAdd(history: OverAllGameHistory): Future[Int] = overallGameHistory.add(history)
}