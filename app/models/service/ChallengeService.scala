package models.service

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalTime, LocalDate, LocalDateTime }
import java.util.UUID
import java.time.{ Instant, ZoneId }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, Challenge, ChallengeHistory }
import models.repo.{ ChallengeRepo, ChallengeHistoryRepo }

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton
class ChallengeService @Inject()(
  challenge: ChallengeRepo,
  history: ChallengeHistoryRepo) {
  private val defaultTimeZone: ZoneId = ZoneId.systemDefault

  def paginatedResult[T >: Challenge](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- challenge.findAll(limit, offset)
      size <- challenge.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // get 12:00 AM of the day based on the date...
  def getChallenge(date: Instant): Future[Option[ChallengeHistory]] = {
    // // get todays local time..
    // val today: LocalDate = LocalDate.now(defaultTimeZone)
    // // instance of MIDNIGHT time
    // val midnight: LocalTime = LocalTime.MIDNIGHT
    // // get todays midnight time by adding instance and todays time
    // val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)

    // Scenario: get midnight based on input time and date...
    // convert Instant to LocalDate
    val today = date.atZone(defaultTimeZone).toLocalDate()
    val midnight: LocalTime = LocalTime.MIDNIGHT
    val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)
    // convert LocalDatetime to Instant
    val todayInstant: Instant = todayMidnight.atZone(defaultTimeZone).toInstant()
    // val todayEpoch: Long = todayInstant.getEpochSecond
    history.findByDate(todayInstant)
  }
  // def getChallengeByDaily(start: Instant, limit: Int, offset: Int): Future[JsValue] = {
  //   try {
  //     for {
  //       txs <- challenge.findByDaily(start.getEpochSecond, limit, offset)
  //       size <- challengeRepo.getSize()
  //       hasNext <- Future(size - (offset + limit) > 0)
  //     } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  //   } catch {
  //     case e: Throwable => Future(Json.obj("err" -> e.toString))
  //   }
  // }
}