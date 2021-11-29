package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, RankingHistory, RankProfit, RankPayout, RankWagered, RankMultiplier }
import models.repo.RankingHistoryRepo
import utils.SystemConfig._

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

    Await.ready(for {
      history <- rankingHistoryRepo.getHistoryByDateRange(start, end)
      calc <- calculateRankHistory(history)
    } yield (calc), Duration.Inf)
  }

  def getRankingHistory(): Future[RankingHistory] = {
    val lengthOfMonth: Int = LocalDate.now(defaultTimeZone).lengthOfMonth
    val start: Long = dateNowPlusDaysUTC(-lengthOfMonth).getEpochSecond
    val end: Long = instantNowUTC().getEpochSecond

    Await.ready(for {
      history <- rankingHistoryRepo.getHistoryByDateRange(start, end)
      calc <- calculateRankHistory(history)
    } yield (calc), Duration.Inf)
  }

  // ranking history by 30 days range
  def calculateRankHistory(v: Seq[RankingHistory]): Future[RankingHistory] = {
    for {
      profit <- Future.successful {
        try {
          v.map(_.profits).flatten.groupBy(x => (x.id, x.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalprofit = seq.asInstanceOf[Seq[RankProfit]].map(_.profit).sum
            RankProfit(id, username, totalbet, totalprofit)
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
          v.map(_.payouts).flatten.groupBy(x => (x.id, x.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalpayout = seq.asInstanceOf[Seq[RankPayout]].map(_.payout).sum
            RankPayout(id, username, totalbet, totalpayout)
          }
          .toSeq
          .filter(_.payout > 0)
          .take(10)
        } catch {
          case e: Throwable => Seq.empty
        }
      }
      wagered <- Future.successful {
        try {
          v.map(_.wagered).flatten.groupBy(x => (x.id, x.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankWagered]].map(_.wagered).sum
            RankWagered(id, username, totalbet, totalwagered)
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
          v.map(_.multipliers).flatten.groupBy(x => (x.id, x.username)).map { case ((id, username), seq) =>
            val totalbet = seq.map(_.bet).sum
            val totalwagered = seq.asInstanceOf[Seq[RankMultiplier]].map(_.multiplier).sum
            RankMultiplier(id, username, totalbet, totalwagered)
          }
          .toSeq
          .filter(_.multiplier > 0)
          .take(10)
        } catch {
          case _: Throwable => Seq.empty
        }
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