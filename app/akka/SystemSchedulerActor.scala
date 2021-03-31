package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ UUID, Calendar }
import java.time._
import scala.util.{ Success, Failure, Random }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import Ordering.Double.IeeeOrdering
import akka.util.Timeout
import utils.Config
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import akka.common.objects.{
        ChallengeScheduler,
        ProcessOverAllChallenge,
        DailyTaskScheduler,
        CreateNewDailyTask,
        RankingScheduler }
import models.domain.{
        Challenge,
        OutEvent,
        Game,
        ChallengeTracker,
        ChallengeHistory,
        Task,
        TaskHistory,
        RankProfit,
        RankPayout,
        RankWagered,
        RankMultiplier,
        GQGameHistory,
        THGameHistory,
        UserAccount,
        RankingHistory,
        OverAllGameHistory }
import models.repo.{
        ChallengeRepo,
        GameRepo,
        ChallengeTrackerRepo,
        ChallengeHistoryRepo,
        TaskRepo,
        TaskHistoryRepo,
        DailyTaskRepo,
        UserAccountRepo,
        RankingHistoryRepo,
        OverAllGameHistoryRepo }

object SystemSchedulerActor {
  var currentChallengeGame: Option[UUID] = None
  var isIntialized: Boolean = false
  def props(
            gameRepo: GameRepo,
            challengeRepo: ChallengeRepo,
            challengeHistoryRepo: ChallengeHistoryRepo,
            challengeTrackerRepo: ChallengeTrackerRepo,
            taskRepo: TaskRepo,
            dailyTaskRepo: DailyTaskRepo,
            taskHistoryRepo: TaskHistoryRepo,
            overAllGameHistory: OverAllGameHistoryRepo,
            userAccountRepo: UserAccountRepo,
            rankingHistoryRepo: RankingHistoryRepo,
            )(implicit system: ActorSystem) =
    Props(classOf[SystemSchedulerActor],
          gameRepo,
          challengeRepo,
          challengeHistoryRepo,
          challengeTrackerRepo,
          taskRepo,
          dailyTaskRepo,
          taskHistoryRepo,
          overAllGameHistory,
          userAccountRepo,
          rankingHistoryRepo,
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
                                      taskHistoryRepo: TaskHistoryRepo,
                                      overAllGameHistory: OverAllGameHistoryRepo,
                                      userAccountRepo: UserAccountRepo,
                                      rankingHistoryRepo: RankingHistoryRepo,
                                      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC

  override def preStart: Unit = {
    super.preStart
    // check if intializer is the SchedulerActor module..
    system.actorSelection("/user/SystemSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!SystemSchedulerActor.isIntialized) {
          // 24hrs Scheduler at 12:00 AM daily
          // any time the system started it will start at 12:AM
          val dailySchedInterval: FiniteDuration = { Config.DEFAULT_SYSTEM_SCHEDULER_TIMER }.hours
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
            self ! RankingScheduler
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
      val createdAt: Long = startOfDay.atZone(defaultTimeZone).toInstant().getEpochSecond
      val expiredAt: Long = createdAt + ((60 * 60 * 24) - 1)
      // val todayEpoch: Long = todayInstant.getEpochSecond
      // check if challenge already for today else do nothing..
      challengeRepo.existByDate(createdAt).map { isCreated =>
        if (!isCreated) {
          for {
            // remove currentChallengeGame and shuffle the result
            availableGames <- gameRepo
              .all()
              .map(games => Random.shuffle(games.filterNot(_.id == SystemSchedulerActor.currentChallengeGame.getOrElse(None))))
            // get head, and create new Challenge for the day
            _ <- Future.successful {
              try {
                val game: Game = availableGames.head
                val newChallenge = new Challenge(UUID.randomUUID,
                                                game.id,
                                                "Challenge content is different every day, use your ingenuity to get the first place.",
                                                createdAt,
                                                expiredAt)

                SystemSchedulerActor.currentChallengeGame = Some(game.id)
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
      val yesterday: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).plusDays(-1).toInstant()
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
                                                Instant.ofEpochSecond(v.created_at),
                                                Instant.ofEpochSecond(Instant.ofEpochSecond(v.created_at).getEpochSecond + ((60 * 60 * 24) - 1)))
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
      self ! (CreateNewDailyTask, yesterday)

    case (CreateNewDailyTask, yesterday: Instant) =>
      val startOfDay: Long = yesterday.getEpochSecond + (60 * 60 * 24)
      // val startOfDay: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).toInstant()
      taskRepo.existByDate(Instant.ofEpochSecond(startOfDay)).map { isCreated =>
        if (!isCreated) {
          for {
            // remove currentChallengeGame and shuffle the result
            availableGames <- gameRepo.all()
            // get head, and create new Challenge for the day
            _ <- Future.successful {
              try {
                val tasks: Seq[UUID] = availableGames.map(_.id)
                taskRepo.add(new Task(UUID.randomUUID, tasks, startOfDay))
              } catch {
                case e: Throwable => println("Error: No games available")
              }
            }
          } yield ()
        }
      }

    case ProcessOverAllChallenge(expiredAt) =>
      // always process when time is equals or greater to its current time..
      // if (Instant.now.getEpochSecond >= expiredAt) {
        for {
          // get all challenge result
          tracked <- challengeTrackerRepo.all()
          // calculate and get highest wagered and take top 10 results
          process <- Future.successful(tracked.sortBy(-_.wagered).take(10))
        } yield (process) match {
          // save result into Challenge history
          case v: Seq[ChallengeTracker] =>
            challengeHistoryRepo
              .add(new ChallengeHistory(UUID.randomUUID, v, Instant.now.getEpochSecond))
              .map(x => if (x > 0) challengeTrackerRepo.clearTable else println("Error: challengeTracker.clearTable"))
        }
      // }

    case RankingScheduler =>
      // get date range to fecth from overall history...
      val start: Long = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).plusDays(-1).toInstant().getEpochSecond
      val end: Long = start + (60 * 60 * 24) - 1
      // fetch overAllGameHistory by date ranges
      for {
        gameHistory <- overAllGameHistory.getByDateRange(start, end)
        // grouped by user -> Map[String, Seq[OverAllGameHistory]]
        grouped <- Future.successful(gameHistory.groupBy(_.info.user))
        profit <- {
          // sum of its bets and amount payout
          Future.sequence(grouped.map { case (user, histories) =>
            // extract bets and amount to (pay or received)
            val tempBets: Seq[(Double, Double)] = histories.map { history =>
              // classify which game per txs and return (bet, amount)
              history.info match {
                case GQGameHistory(name, prediction, result, bet) =>
                  if (result) (bet, 1) else (bet, -1)
                case THGameHistory(name, prediction, result, bet, amount) =>
                  if (prediction == result) (bet, amount) else (bet, -amount)
                // GameType(_, _, _), PaymentType(_, _, _)
                case e => (e.bet, 0)
              }
            }

            val betsSum: Double = tempBets.map(_._1).sum
            val toPayOrReceivedSum: Double = tempBets.map(_._2).sum
            (user, betsSum, (toPayOrReceivedSum - betsSum ))
          }
          .toSeq
          .sortBy(-_._3)
          .take(10)
          .map { v =>
            // get userid using name string..
            userAccountRepo.getByName(v._1).map(_.map(user => RankProfit(user.id, v._2, v._3)).getOrElse(null))
          })
          .map(_.filterNot(_ == null))
        }
        payout <- {
          // sum of its bets and amount payout
          Future.sequence(grouped.map { case (user, histories) =>
            // extract bets and amount to (pay or received)
            val tempBets: Seq[(Double, Double)] = histories.map { history =>
              // classify which game per txs and return (bet, amount)
              history.info match {
                case GQGameHistory(name, prediction, result, bet) =>
                  if (result) (bet, 1) else (bet, -1)
                case THGameHistory(name, prediction, result, bet, amount) =>
                  if (prediction == result) (bet, amount) else (bet, -amount)
                // GameType(_, _, _), PaymentType(_, _, _)
                case e => (e.bet, 0)
              }
            }

            val betsSum: Double = tempBets.map(_._1).sum
            val toPayOrReceivedSum: Double = tempBets.map(_._2).sum
            (user, betsSum, toPayOrReceivedSum)
          }
          .toSeq
          .sortBy(-_._3)
          .take(10)
          .map { v =>
            // get userid using name string..
            userAccountRepo.getByName(v._1).map(_.map(user => RankPayout(user.id, v._2, v._3)).getOrElse(null))
          })
          .map(_.filterNot(_ == null))
        }
        wagered <- {
          // sum of its bets and amount payout
          Future.sequence(grouped.map { case (user, histories) =>
            // extract bets and amount to (pay or received)
            val tempBets: Seq[(Double, Double)] = histories.map { history =>
              // classify which game per txs and return (bet, amount, multiplier)
              history.info match {
                case GQGameHistory(name, prediction, result, bet) =>
                  if (result) (bet, 1) else (bet, -1)
                case THGameHistory(name, prediction, result, bet, amount) =>
                  if (prediction == result) (bet, amount) else (bet, -amount)
                // GameType(_, _, _), PaymentType(_, _, _)
                case e => (e.bet, 0)
              }
            }
            val betsSum: Double = tempBets.map(_._1).sum
            val toPayOrReceivedSum: Double = tempBets.map(_._2).sum
            (user, betsSum, betsSum)
          }
          .toSeq
          .sortBy(-_._3)
          .take(10)
          .map { v =>
            // get userid using name string..
            userAccountRepo.getByName(v._1).map(_.map(user => RankWagered(user.id, v._2, v._3)).getOrElse(null))
          })
          .map(_.filterNot(_ == null))
        }
        multiplier <- {
          // sum of its bets and amount payout
          Future.sequence(grouped.map { case (user, histories) =>
            // extract bets and amount to (pay or received)
            val tempBets: Seq[(Double, Double)] = histories.map { history =>
              // classify which game per txs and return (bet, amount, multiplier)
              history.info match {
                case GQGameHistory(name, prediction, result, bet) =>
                  if (result) (bet, 1) else (bet, 0)
                case THGameHistory(name, prediction, result, bet, amount) =>
                  if (prediction == result) (bet, 1) else (bet, 0)
                // GameType(_, _, _), PaymentType(_, _, _)
                case e => (e.bet, 0)
              }
            }
            val betsSum: Double = tempBets.map(_._1).sum
            val multiplierSum: Double = tempBets.map(_._2).sum
            (user, betsSum, multiplierSum)
          }
          .toSeq
          .sortBy(-_._3)
          .take(10)
          .map { v =>
            // get userid using name string..
            userAccountRepo.getByName(v._1).map(_.map(user => RankMultiplier(user.id, v._2, v._3)).getOrElse(null))
          })
          .map(_.filterNot(_ == null))
        }
        // save ranking to history..
        _ <- Future {
          val rank = RankingHistory(UUID.randomUUID, profit, payout, wagered, multiplier, start)
          // insert into DB, if failed then re-insert
          rankingHistoryRepo.add(rank).map(x => if(x > 0)() else rankingHistoryRepo.add(rank))
        }
      } yield ()

    case _ => ()
  }
}