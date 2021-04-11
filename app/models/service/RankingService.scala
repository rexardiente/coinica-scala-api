package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, RankingHistory, RankProfit, RankPayout, RankWagered, RankMultiplier }
import models.repo.RankingHistoryRepo

@Singleton
class RankingService @Inject()(rankingHistoryRepo: RankingHistoryRepo ) {
  def paginatedResult[T >: RankingHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- rankingHistoryRepo.findAll(limit, offset)
      size <- rankingHistoryRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // prev 24hrs tx results
  def getRankingDaily(): Future[Seq[RankingHistory]] = {
    val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
    val end: Instant = now.plusDays(-1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()

    rankingHistoryRepo.getHistoryByDateRange(
                        end.getEpochSecond,
                        now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond)
  }

  def getRankingHistory(): Future[RankingHistory] = {
    val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
    val end: Instant = now.plusDays(-30).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()

    Await.ready(for {
      history <- rankingHistoryRepo.getHistoryByDateRange(
                                      end.getEpochSecond,
                                      now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond)
      calc <- calculateRankHistory(history)
    } yield (calc), Duration.Inf)
  }

  // ranking history by 30 days range
  def calculateRankHistory(v: Seq[RankingHistory]): Future[RankingHistory] = {
    for {
      profit <- Future.successful {
        try {
          v.map(_.profits).flatten.groupBy(_.user).map { case (id, seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalprofit = seq.asInstanceOf[Seq[RankProfit]].map(_.profit).sum
            RankProfit(id, totalbet, totalprofit)
          }
          .toSeq
          .filter(_.profit > 0)
          .take(10)
        } catch {
          case _: Throwable => Seq.empty
        }
      }
      payout <- Future.successful {
        try {
          v.map(_.payouts).flatten.groupBy(_.user).map { case (id, seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalpayout = seq.asInstanceOf[Seq[RankPayout]].map(_.payout).sum
            RankPayout(id, totalbet, totalpayout)
          }
          .toSeq
          .filter(_.payout > 0)
          .take(10)
        } catch {
          case e: Throwable =>
            println(e)
            Seq.empty
        }
      }
      wagered <- Future.successful {
        try {
          v.map(_.wagered).flatten.groupBy(_.user).map { case (id, seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankWagered]].map(_.wagered).sum
            RankWagered(id, totalbet, totalwagered)
          }
          .toSeq
          .filter(_.wagered > 0)
          .take(10)
        } catch {
          case _: Throwable => Seq.empty
        }
      }
      multiplier <- Future.successful {
        try {
          v.map(_.multipliers).flatten.groupBy(_.user).map { case (id, seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankMultiplier]].map(_.multiplier).sum
            RankMultiplier(id, totalbet, totalwagered)
          }
          .toSeq
          .filter(_.multiplier > 0)
          .take(10)
        } catch {
          case _: Throwable => Seq.empty
        }
      }
    } yield (RankingHistory(UUID.randomUUID, profit, payout, wagered, multiplier, Instant.now.getEpochSecond))
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