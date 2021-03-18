package akka

import javax.inject.{ Inject, Singleton }
import java.util.{ UUID, Calendar }
import java.time._
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
import akka.common.objects.{
        ChallengeScheduler,
        ProcessOverAllChallenge,
        DailyTaskScheduler,
        WeeklyTaskScheduler,
        MonthlyTaskScheduler,
        CreateNewDailyTask }
import models.domain.{
        Challenge,
        OutEvent,
        Game,
        ChallengeTracker,
        ChallengeHistory,
        Task,
        TaskHistory }
import models.repo.{
        ChallengeRepo,
        GameRepo,
        ChallengeTrackerRepo,
        ChallengeHistoryRepo,
        TaskRepo,
        TaskHistoryRepo,
        DailyTaskRepo }

object SystemSchedulerActor {
  var currentChallengeGame: String = ""
  var isIntialized: Boolean = false
  def props(
            gameRepo: GameRepo,
            challengeRepo: ChallengeRepo,
            challengeHistoryRepo: ChallengeHistoryRepo,
            challengeTrackerRepo: ChallengeTrackerRepo,
            taskRepo: TaskRepo,
            dailyTaskRepo: DailyTaskRepo,
            taskHistoryRepo: TaskHistoryRepo,
            )(implicit system: ActorSystem) =
    Props(classOf[SystemSchedulerActor],
          gameRepo,
          challengeRepo,
          challengeHistoryRepo,
          challengeTrackerRepo,
          taskRepo,
          dailyTaskRepo,
          taskHistoryRepo,
          system)
}

@Singleton
class SystemSchedulerActor @Inject()(
                                      gameRepo: GameRepo,
                                      challengeRepo: ChallengeRepo,
                                      challengeHistoryRepo: ChallengeHistoryRepo,
                                      challengeTrackerRepo: ChallengeTrackerRepo,
                                      taskRepo: TaskRepo,
                                      dailyTaskRepo: DailyTaskRepo,
                                      taskHistoryRepo: TaskHistoryRepo
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC
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
          system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => {
            self ! ChallengeScheduler
            self ! DailyTaskScheduler
          })
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
      // val today = Instant.now().atZone(defaultTimeZone).toLocalDate()
      // val midnight: LocalTime = LocalTime.MIDNIGHT
      // val todayMidnight: LocalDateTime = LocalDateTime.of(today, midnight)
      val startOfDay: LocalDateTime = LocalDate.now().atStartOfDay()
      // convert LocalDatetime to Instant
      val createdAt: Instant = startOfDay.atZone(defaultTimeZone).toInstant()
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
                val newChallenge = new Challenge(UUID.randomUUID,
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
    case DailyTaskScheduler =>
      val startOfDay: LocalDateTime = LocalDate.now().atStartOfDay()
      val yesterday: Instant = startOfDay.atZone(defaultTimeZone).plusDays(-1).toInstant()
      // process first all available task before creating new tasks
      val trackedFailedInsertion = ListBuffer.empty[TaskHistory]

      taskRepo.getDailyTaskByDate(yesterday).map(_.map { v =>
        for {
          tracked <- dailyTaskRepo.all()
          _ <- Future.successful {
            tracked.map(x => {
              val taskHistory = new TaskHistory(UUID.randomUUID,
                                                v.id,
                                                x.game_id,
                                                x.user,
                                                x.game_count,
                                                v.created_at,
                                                Instant.ofEpochSecond(v.created_at.getEpochSecond + ((60 * 60 * 24) - 1)))
              // insert and if failed do insert 1 more time..
              taskHistoryRepo.add(taskHistory).map(isAdded => if(isAdded > 0)() else trackedFailedInsertion.addOne(taskHistory) )
            })
          }
          _ <- Future.successful(dailyTaskRepo.clearTable)
        } yield ()
      })
      // re-insert failed txs on DB
      // TODO: if failed again make new DB records to track and manually insert it..
      trackedFailedInsertion.map(taskHistoryRepo.add)
      Thread.sleep(2000)
      self ! CreateNewDailyTask

    case CreateNewDailyTask =>
      val startOfDay: LocalDateTime = LocalDate.now().atStartOfDay()
      val createdAt: Instant = startOfDay.atZone(defaultTimeZone).toInstant()
      // val expiredAt: Long = createdAt.getEpochSecond + ((60 * 60 * 24) - 1)
      taskRepo.existByDate(createdAt).map { isCreated =>
        if (!isCreated) {
          for {
            // remove currentChallengeGame and shuffle the result
            availableGames <- gameRepo.all()
            // get head, and create new Challenge for the day
            _ <- Future.successful {
              try {
                val tasks: Seq[UUID] = availableGames.map(_.id)
                taskRepo.add(new Task(UUID.randomUUID, tasks, createdAt))
              } catch {
                case e: Throwable => println("Error: No games available")
              }
            }
          } yield ()
        }
      }
    // get all txs at TaskTracker history in this week
    // process and get those who passed the required limit
    // and add 10 VIP points
    case WeeklyTaskScheduler =>
    // get all txs at TaskTracker history in this month
    // process and get those who passed the required limit
    // and add 50 VIP points
    case MonthlyTaskScheduler =>
      // val today: LocalDate = LocalDate.now().atStartOfDay().toLocalDate()
      // val firstDayOfMonth: LocalDate = today.withDayOfMonth(1)
      // val lastDayOfMonth: LocalDate = today.withDayOfMonth(today.lengthOfMonth())

    case ProcessOverAllChallenge(expiredAt) =>
      // always process when time is equals or greater to its current time..
      if (Instant.now.getEpochSecond >= expiredAt) {
        for {
          // get all challenge result
          tracked <- challengeTrackerRepo.all()
          // calculate and get highest wagered and take top 10 results
          process <- Future.successful(tracked.sortBy(-_.wagered).take(10))
        } yield (process) match {
          // save result into Challenge history
          case v: Seq[ChallengeTracker] =>
            challengeHistoryRepo
              .add(new ChallengeHistory(UUID.randomUUID, v.head.challengeID, v, Instant.now))
              .map(x => if (x > 0) challengeTrackerRepo.clearTable else println("Error: challengeTracker.clearTable"))
        }
      }

    case _ =>
  }
}