package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ LocalTime, Instant, ZoneId, LocalDate, LocalDateTime }
import scala.util.{ Success, Failure, Random }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import Ordering.Double.IeeeOrdering
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import akka.common.objects.{ ChallengeScheduler, ProcessOverAllChallenge }
import models.domain.{ Challenge, OutEvent, Game, ChallengeTracker, ChallengeHistory }
import models.repo.{ ChallengeRepo, GameRepo, ChallengeTrackerRepo, ChallengeHistoryRepo }

object SystemSchedulerActor {
  var currentChallengeGame: String = ""
  var isIntialized: Boolean = false
  def props(
            gameRepo: GameRepo,
            challengeRepo: ChallengeRepo,
            historyRepo: ChallengeHistoryRepo,
            trackerRepo: ChallengeTrackerRepo
            )(implicit system: ActorSystem) =
    Props(classOf[SystemSchedulerActor], gameRepo, challengeRepo, historyRepo, trackerRepo, system)
}

@Singleton
class SystemSchedulerActor @Inject()(
                                      gameRepo: GameRepo,
                                      challengeRepo: ChallengeRepo,
                                      historyRepo: ChallengeHistoryRepo,
                                      trackerRepo: ChallengeTrackerRepo
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val defaultTimeZone: ZoneId = ZoneId.systemDefault
  private val dynamicBroadcast: ActorRef = system.actorOf(Props(classOf[DynamicBroadcastActor], None, system))

  override def preStart: Unit = {
    super.preStart
    // check if intializer is the SchedulerActor module..
    system.actorSelection("/user/SystemSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!SystemSchedulerActor.isIntialized) {
          // 24hrs Scheduler at 12:00 AM daily
          // any time the system started it will start at 12:AM
          val dailySchedInterval: FiniteDuration = 24.hours
          val dailySchedDelay   : FiniteDuration = {
              val time = LocalTime.of(0, 0).toSecondOfDay
              val now = LocalTime.now().toSecondOfDay
              val fullDay = 60 * 60 * 24
              val difference = time - now
              if (difference < 0) {
                fullDay + difference
              } else {
                time - now
              }
            }.seconds
          system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => self ! ChallengeScheduler)
          // set true if actor already initialized
          SystemSchedulerActor.isIntialized = true
          log.info("System Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    // run scehduler every midnight of day..
    case ChallengeScheduler =>
      val today = Instant.now().atZone(defaultTimeZone).toLocalDate()
      val midnight: LocalTime = LocalTime.MIDNIGHT
      val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)
      // convert LocalDatetime to Instant
      val createdAt: Instant = todayMidnight.atZone(defaultTimeZone).toInstant()
      val expiredAt: Long = createdAt.getEpochSecond + ((60 * 60 * 24) - 1)
      // val todayEpoch: Long = todayInstant.getEpochSecond
      // check if challenge already for today else do nothing..
      challengeRepo.existByDate(createdAt).map { isCreated =>
        if (!isCreated) {
          for {
            // remove currentChallengeGame and shuffle the result
            availableGames <- gameRepo
            .all()
            .map(games => Random.shuffle(games.filterNot(_.name == SystemSchedulerActor.currentChallengeGame)))
            // get head, and create new Challenge for the day
            _ <- Future.successful {
              try {
                val newChallenge: Challenge = Challenge(UUID.randomUUID,
                                                      availableGames.head.id,
                                                      "Challenge content is different every day, use you ingenuity to get the first place.",
                                                      createdAt,
                                                      Instant.ofEpochSecond(expiredAt))

              challengeRepo.add(newChallenge)
              } catch {
                case e: Throwable => println("Error: No games available")
              }
            }
            // after creating new challenge..
            // calculate overe all challenge and save to Challengehistory for tracking top ranks
          } yield (self ! ProcessOverAllChallenge(expiredAt))
        }
        // else self ! ProcessOverAllChallenge(expiredAt)
      }

    case ProcessOverAllChallenge(expiredAt) =>
      // always process when time is equals or greater to its current time..
      if (Instant.now.getEpochSecond >= expiredAt) {
        for {
          // get all challenge result
          tracker <- trackerRepo.all()
          // calculate and get highest wagered and take top 10 results
          process <- Future.successful(tracker.sortBy(-_.wagered).take(10))
        } yield (process) match {
          // save result into Challenge history
          case v: Seq[ChallengeTracker] =>
            historyRepo
              .add(new ChallengeHistory(UUID.randomUUID, v.head.challengeID, v, Instant.now))
              .map(x => if (x > 0) trackerRepo.clearTable else println("Error: trackerRepo.clearTable"))
        }
      }

    case _ =>
  }
}