package models.service

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalTime, LocalDate, LocalDateTime, Instant, ZoneId, ZoneOffset }
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain.{ PaginatedResult, Challenge, ChallengeTracker, ChallengeHistory }
import models.repo.{ ChallengeRepo, ChallengeTrackerRepo, ChallengeHistoryRepo }

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton
class ChallengeService @Inject()(
  challenge: ChallengeRepo,
  tracker: ChallengeTrackerRepo,
  history: ChallengeHistoryRepo,
  userAccount: UserAccountService) {
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC

  def paginatedResult[T >: Challenge](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- challenge.all()
      size <- challenge.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // get 12:00 AM of the day based on the date...
  def getChallenge(date: Option[Instant]): Future[Option[ChallengeHistory]] = {
    // // get todays local time..
    // val today: LocalDate = LocalDate.now(defaultTimeZone)
    // // instance of MIDNIGHT time
    // val midnight: LocalTime = LocalTime.MIDNIGHT
    // // get todays midnight time by adding instance and todays time
    // val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)

    // Scenario: get midnight based on input time and date...
    // convert Instant to LocalDate
    val today = date.map(_.atZone(defaultTimeZone)).getOrElse(Instant.now.atZone(defaultTimeZone).plusDays(-1)).toLocalDate()
    val midnight: LocalTime = LocalTime.MIDNIGHT
    val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)
    // convert LocalDatetime to Instant
    val todayEpoch: Long = todayMidnight.atZone(defaultTimeZone).toInstant().getEpochSecond
    // val todayEpoch: Long = todayInstant.getEpochSecond
    history.findByDate(todayEpoch)
  }
  def getDailyRanksChallenge(): Future[JsArray] = {
    for {
      top10Res <- tracker.all.map(_.sortBy(-_.wagered).take(10))
      // add username field on the JSON response..
      newJSONObj <- Future.sequence {
        top10Res.map { seq =>
          userAccount
            .getAccountByID(seq.user)
            .map(x => seq.toJson.as[JsObject] + ("username" -> Json.toJson(x.map(_.username))))
        }
      }
    } yield (JsArray(newJSONObj))
  }
}