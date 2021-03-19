package models.service

import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.{ Instant, ZoneId }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, ReferralHistory, UserAccount }
import models.repo.{ UserAccountRepo, ReferralHistoryRepo }

@Singleton
class ReferralHistoryService @Inject()(userAccountRepo: UserAccountRepo, referralRepo: ReferralHistoryRepo) {
  def paginatedResult[T >: ReferralHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {

	  for {
      tasks <- referralRepo.findAll(limit, offset)
      size <- referralRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getByCode(code: String): Future[Seq[ReferralHistory]] =
    for {
      // check if user exists else return seq empty
      isExists <- userAccountRepo.isCodeExist(code)
      result <- if (isExists) referralRepo.getByCode(code) else Future(Seq.empty)
    } yield result
  // check if user exists and not referred by someone
  // make sure code doenst belong to itself
  def applyReferralCode(code: String, appliedBy: String): Future[Int] =
    for {
      isCodeOwnedBySelf <- userAccountRepo.isCodeOwnedBy(code, appliedBy)
      hasNoReferral <- userAccountRepo.hasNoReferral(appliedBy)
      result <- {
        if (!isCodeOwnedBySelf && hasNoReferral != None) {
          for {
            _ <- userAccountRepo.update(hasNoReferral.get)
            _ <- referralRepo.add(new ReferralHistory(UUID.randomUUID, code, appliedBy, Instant.now))
          } yield (1)
        }
        else Future(0)
      }
    } yield result
  // def getReferralByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
  // 	try {
  // 		for {
	 //      txs <- referralRepo.findByDateRange(
	 //      	start.getEpochSecond,
	 //      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),
	 //      	limit,
	 //      	offset)
	 //      size <- referralRepo.getSize()
	 //      hasNext <- Future(size - (offset + limit) > 0)
	 //    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  // 	} catch {
  // 		case e: Throwable => Future(Json.obj("err" -> e.toString))
  // 	}
  // }

  // def getReferralByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {

  //   try {
  //     for {
  //       txs <- referralRepo.findByDaily(start.getEpochSecond, limit, offset)
  //       size <- referralRepo.getSize()
  //       hasNext <- Future(size - (offset + limit) > 0)
  //     } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  //   } catch {
  //     case e: Throwable => Future(Json.obj("err" -> e.toString))
  //   }
  // }
}