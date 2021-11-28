package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain.{ PaginatedResult, Challenge, ChallengeTracker, ChallengeHistory }
import models.repo.{ ChallengeRepo, ChallengeTrackerRepo, ChallengeHistoryRepo, TaskRepo }

// import java.time.{ Instant, ZoneId }
// Instant.now().atZone(ZoneId.systemDefault)

@Singleton
class ChallengeService @Inject()(
  taskRepo: TaskRepo,
  challenge: ChallengeRepo,
  tracker: ChallengeTrackerRepo,
  history: ChallengeHistoryRepo,
  userAccount: UserAccountService) {
  def paginatedResult[T >: Challenge](limit: Int, offset: Int): Future[PaginatedResult[T]] = {
	  for {
      tasks <- challenge.all()
      size <- challenge.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  // getChallenge version 2
  // get 2nd to the latest data from task table..
  // fetch all challenge history under this span of time
  // process and return to UI
  def getChallenge(): Future[JsValue] = {
    for {
      hasTask <- taskRepo.getTaskWithOffset(1)
      response <- {
        hasTask.map { task =>
          for {
            history <- history.findByDate(task.created_at)
            newJSONObj <- {
              history.map { x =>
                Future.sequence(x.rank_users.map { rank =>
                  // add username field on the JSON response..
                  userAccount
                    .getAccountByID(rank.user)
                    .map(acc => rank.toJson.as[JsObject] + ("username" -> Json.toJson(acc.map(_.username))))
                })
                .map(JsArray(_))
              }.getOrElse(Future(JsNull))
            }
          } yield (newJSONObj)
        }
        .getOrElse(Future.successful(JsNull))
      }
    } yield (response)
  }
  // // get 12:00 AM of the day based on the date...
  // def getChallenge(date: Option[Instant]): Future[JsValue] = {
  //   // // get todays local time..
  //   // val today: LocalDate = LocalDate.now(defaultTimeZone)
  //   // // instance of MIDNIGHT time
  //   // val midnight: LocalTime = LocalTime.MIDNIGHT
  //   // // get todays midnight time by adding instance and todays time
  //   // val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)

  //   // Scenario: get midnight based on input time and date...
  //   // convert Instant to LocalDate
  //   val today = date.map(_.atZone(defaultTimeZone)).getOrElse(Instant.now.atZone(defaultTimeZone).plusDays(-1)).toLocalDate()
  //   val midnight: LocalTime = LocalTime.MIDNIGHT
  //   val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)
  //   // convert LocalDatetime to Instant
  //   val todayEpoch: Long = todayMidnight.atZone(defaultTimeZone).toInstant().getEpochSecond
  //   // val todayEpoch: Long = todayInstant.getEpochSecond
  //   for {
  //     history <- history.findByDate(todayEpoch)
  //     process <- {
  //       history.map { x =>
  //         Future.sequence(x.rank_users.map { rank =>
  //           // add username field on the JSON response..
  //           userAccount
  //             .getAccountByID(rank.user)
  //             .map(acc => rank.toJson.as[JsObject] + ("username" -> Json.toJson(acc.map(_.username))))
  //         })
  //         .map(JsArray(_))
  //       }.getOrElse(Future(JsNull))
  //     }
  //   } yield (process)
  // }
  def getDailyRanksChallenge(): Future[JsArray] = {
    for {
      top10Res <- tracker.all.map(_.sortBy(-_.wagered).take(10))
      // add username field on the JSON response..
      newJSONObj <- Future.sequence {
        top10Res.map { tracked =>
          userAccount
            .getAccountByID(tracked.user)
            .map(x => tracked.toJson.as[JsObject] + ("username" -> Json.toJson(x.map(_.username))))
        }
      }
      // to make sure that Challenge scheduler has not executed early
      hasTask <- taskRepo.getTaskWithOffset(0)
      hasHistory <- {
        hasTask.map { task =>
          for {
            history <- history.findByDate(task.created_at)
            newJSONObj <- {
              history.map { x =>
                Future.sequence(x.rank_users.map { rank =>
                  // add username field on the JSON response..
                  userAccount
                    .getAccountByID(rank.user)
                    .map(acc => rank.toJson.as[JsObject] + ("username" -> Json.toJson(acc.map(_.username))))
                })
                .map(JsArray(_))
              }.getOrElse(Future(JsArray()))
            }
          } yield (newJSONObj)
        }
        .getOrElse(Future.successful(JsArray()))
      }
    } yield (JsArray(newJSONObj) ++ hasHistory)
  }
}