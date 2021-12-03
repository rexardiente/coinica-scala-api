package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import Ordering.Double.IeeeOrdering
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, RankingHistory, RankProfit, RankPayout, RankWagered, RankMultiplier, ChallengeTracker, RankType }
import models.repo.{ ChallengeTrackerRepo, UserAccountRepo, RankingHistoryRepo }
import utils.SystemConfig._

@Singleton
class RankingService @Inject()(
      challengeTrackerRepo: ChallengeTrackerRepo,
      userAccountRepo: UserAccountRepo,
      rankingHistoryRepo: RankingHistoryRepo) {
  def paginatedResult[T >: RankingHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- rankingHistoryRepo.findAll(limit, offset)
      size <- rankingHistoryRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }
  // prev 24hrs tx results
  // def getRankingDaily(): Future[Option[RankingHistory]] = {
  //   val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
  //   val end: Instant = now.plusDays(-1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(defaultTimeZone)

  //   rankingHistoryRepo.getHistoryByDateRange(
  //                       end.getEpochSecond,
  //                       now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond)
  //                     .map(_.headOption)
  // }
  def getRankingDaily(): Future[RankingHistory] = {
    val start: Long = dateNowPlusDaysUTC(-1).getEpochSecond
    val end: Long = instantNowUTC().getEpochSecond

    for {
      tracker <- challengeTrackerRepo.all()
      calc <- calculateRank(tracker)
    } yield (calc)
  }

  def calculateRank(data: Seq[ChallengeTracker]): Future[RankingHistory] = {
    for {
      profit <- Future.sequence {
        data.map { case ChallengeTracker(id, bets, wagered, ratio, points, payout, multiplier) => (id, bets, (payout - bets)) }
            .filter(_._3 > 0) // remove use whos not having enough payout
            .sortBy(-_._3) // sort result by payout
            .take(10) // take only 10 result
            .map { case (id, bets, profit) =>
              userAccountRepo
                .getByID(id)
                .map(_.map(account => RankProfit(account.id, account.username, bets, profit)))
            }
      }
      payout <- Future.sequence {
        data.map { case ChallengeTracker(id, bets, wagered, ratio, points, payout, multiplier) => (id, bets, payout) }
            .filter(_._3 > 0)
            .sortBy(-_._3)
            .take(10)
            .map { case (id, bets, payout) =>
              userAccountRepo
                .getByID(id)
                .map(_.map(account => RankPayout(account.id, account.username, bets, payout)))
            }
      }
      wagered <- Future.sequence {
        data.map { case ChallengeTracker(id, bets, wagered, ratio, points, payout, multiplier) => (id, bets, wagered) }
            .filter(_._3 > 0)
            .sortBy(-_._3)
            .take(10)
            .map { case (id, bets, wagered) =>
              userAccountRepo
                .getByID(id)
                .map(_.map(account => RankWagered(account.id, account.username, bets, wagered)))
            }
      }
      // total win size
      multiplier <- Future.sequence {
        data.map { case ChallengeTracker(id, bets, wagered, ratio, points, payout, multiplier) => (id, bets, multiplier) }
            .filter(_._3 > 0)
            .sortBy(-_._3)
            .take(10)
            .map { case (id, bets, multiplier) =>
              userAccountRepo
                .getByID(id)
                .map(_.map(account => RankMultiplier(account.id, account.username, bets, multiplier)))
            }
      }
      rankedData <- Future.successful {
        val start: Long = dateNowPlusDaysUTC(-1).getEpochSecond
        //  remove null values from the list..
        val aProfit: Seq[RankType] = removeNoneValue[RankType](profit)
        val aPayout: Seq[RankType] = removeNoneValue[RankType](payout)
        val aWagered: Seq[RankType] = removeNoneValue[RankType](wagered)
        val aMultiplier: Seq[RankType] = removeNoneValue[RankType](multiplier)
        RankingHistory(aProfit, aPayout, aWagered, aMultiplier, start)
      }
    } yield (rankedData)
  }
  // remove null values from list[RankType]
  private def removeNoneValue[T >: RankType](v: Seq[Option[T]]): Seq[T] = v.map(_.getOrElse(null))

  def getRankingHistory(): Future[RankingHistory] = {
    val lengthOfMonth: Int = LocalDate.now(defaultTimeZone).lengthOfMonth
    val start: Long = dateNowPlusDaysUTC(-lengthOfMonth).getEpochSecond
    val end: Long = instantNowUTC().getEpochSecond

    for {
      history <- rankingHistoryRepo.getHistoryByDateRange(start, end)
      calc <- calculateRankHistory(history)
    } yield (calc)
  }
  // ranking history by 30 days range
  def calculateRankHistory(v: Seq[RankingHistory]): Future[RankingHistory] = {
    for {
      profit <- Future.successful {
        v.map(_.profits).flatten.groupBy(history => (history.id, history.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalprofit = seq.asInstanceOf[Seq[RankProfit]].map(_.profit).sum
            RankProfit(id, username, totalbet, totalprofit)
          }
          .toSeq
          .filter(_.profit > 0)
          .take(10)
      }
      payout <- Future.successful {
        v.map(_.payouts).flatten.groupBy(history => (history.id, history.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalpayout = seq.asInstanceOf[Seq[RankPayout]].map(_.payout).sum
            RankPayout(id, username, totalbet, totalpayout)
          }
          .toSeq
          .filter(_.payout > 0)
          .take(10)
      }
      wagered <- Future.successful {
        v.map(_.wagered).flatten.groupBy(history => (history.id, history.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankWagered]].map(_.wagered).sum
            RankWagered(id, username, totalbet, totalwagered)
          }
          .toSeq
          .filter(_.wagered > 0)
          .take(10)
      }
      multiplier <- Future.successful {
        v.map(_.multipliers).flatten.groupBy(history => (history.id, history.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankMultiplier]].map(_.multiplier).sum
            RankMultiplier(id, username, totalbet, totalwagered)
          }
          .toSeq
          .filter(_.multiplier > 0)
          .take(10)
      }
    } yield (RankingHistory(v.headOption.map(_.id).getOrElse(UUID.randomUUID), profit, payout, wagered, multiplier, instantNowUTC().getEpochSecond))
  }
  // def getRankingByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  // 	try {
  // 		for {
	 //      txs <- rankingRepo.findByDateRange(
	 //      	start.getEpochSecond,
	 //      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	 //      	limit,
	 //      	offset)
	 //      size <- rankingRepo.getSize()
	 //      hasNext <- Future(size - (offset + limit) > 0)
	 //    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  // 	} catch {
  // 		case e: Throwable => Future(Json.obj("err" -> e.toString))
  // 	}
  // }
  // def getRankingByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {
  //   try {
  //     for {
  //       txs <- rankingRepo.findByDaily(start.getEpochSecond, limit, offset)
  //       size <- rankingRepo.getSize()
  //       hasNext <- Future(size - (offset + limit) > 0)
  //     } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  //   } catch {
  //     case e: Throwable => Future(Json.obj("err" -> e.toString))
  //   }
  // }
}