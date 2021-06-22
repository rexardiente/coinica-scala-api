package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ UUID, Calendar }
import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, ZoneOffset, ZoneId, ZonedDateTime }
import scala.util.{ Success, Failure, Random }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import Ordering.Double.IeeeOrdering
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import akka.util.Timeout
import utils.Config
import play.api.libs.ws.WSClient
import play.api.libs.json._
import akka.common.objects._
import models.domain._
import models.repo._
import models.service.UserAccountService
import models.domain.enum._
import utils.lib.MultiCurrencyHTTPSupport

object SystemSchedulerActor {
  var currentChallengeGame: Option[UUID] = None
  var isIntialized: Boolean = false
  val walletTransactions = HashMap.empty[String, ETHWalletTxEvent]
  def props(
            userAccountService: UserAccountService,
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
            vipUserRepo: VIPUserRepo,
            httpSupport: MultiCurrencyHTTPSupport,
            )(implicit system: ActorSystem) =
    Props(classOf[SystemSchedulerActor],
          userAccountService,
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
          vipUserRepo,
          httpSupport,
          system)
}

@Singleton
class SystemSchedulerActor @Inject()(userAccountService: UserAccountService,
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
                                    vipUserRepo: VIPUserRepo,
                                    httpSupport: MultiCurrencyHTTPSupport,
                                    @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC

  override def preStart: Unit = {
    super.preStart
    // keep alive connection
    // https://stackoverflow.com/questions/13700452/scheduling-a-task-at-a-fixed-time-of-the-day-with-akka
    akka.stream.scaladsl.Source.tick(0.seconds, 20.seconds, "SystemSchedulerActor").runForeach(n => ())
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
          // load default SC users/account to avoid adding into user accounts table
          WebSocketActor.subscribers.addOne(Config.GQ_CODE, self)
          WebSocketActor.subscribers.addOne(Config.TH_CODE, self)
          WebSocketActor.subscribers.addOne(Config.MJHilo_CODE, self)
          // ETH and USDC wallet tx details checker,
          // limit checking of tx details failed..
          akka
            .stream
            .scaladsl
            .Source
            .tick(0.seconds, 5.minutes, "SystemSchedulerActor")
            .runForeach(n => {
              // schedule every 5mins
              // add to scheduler for tx details
              SystemSchedulerActor.walletTransactions.foreach { data: (String, ETHWalletTxEvent) =>
                for {
                  // check account information..
                  hasAccount <- userAccountService.getUserAccountWallet(data._2.account_id)
                  _ <- {
                    hasAccount.map { account =>
                      data._2.currency match {
                      case "USDC" | "ETH" =>
                        for {
                          txDetails <- httpSupport.getETHTxInfo(data._1, data._2.currency)
                          // update DB and history..
                          _ <- Future.successful {
                            txDetails.map { detail =>
                              if (data._2.tx_type == "DEPOSIT")
                                userAccountService.addBalanceByCurrency(data._2.account_id, data._2.currency, detail.result.value.toDouble)
                              else
                                userAccountService.deductBalanceByCurrency(data._2.account_id, data._2.currency, detail.result.value.toDouble, detail.result.gasPrice)
                            }
                          }
                          _ <- Future.successful {
                            txDetails.map { detail =>
                              userAccountService.saveUserWalletHistory(
                                new models.domain.wallet.support.UserAccountWalletHistory(data._2.tx_hash,
                                                                                          data._2.account_id,
                                                                                          data._2.currency,
                                                                                          data._2.tx_type,
                                                                                          detail.result,
                                                                                          Instant.now))
                            }
                          }
                          _ <- Future.successful {
                            txDetails.map(detail => SystemSchedulerActor.walletTransactions.remove(data._1))
                          }
                        } yield ()

                       case _ => Future(None)
                      }
                    }
                    .getOrElse(Future(None))
                  }
                } yield ()
              }
            })

          log.info("System Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    // run scehduler every midnight of day..
    case ChallengeScheduler =>
      val yesterday: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).plusDays(-1).toInstant()
      // update all earned points into users Account
      Await.ready(processChallengeTrackerAndEarnedVIPPoints(yesterday), Duration.Inf)
      Thread.sleep(2000)
      // ProcessOverAllChallenge(yesterday.getEpochSecond)
      // val startOfDay: LocalDateTime = LocalDate.now().atStartOfDay()
      // // convert LocalDatetime to Instant
      // val createdAt: Long = startOfDay.atZone(defaultTimeZone).toInstant().getEpochSecond
      // val expiredAt: Long = createdAt + ((60 * 60 * 24) - 1)
      // // val todayEpoch: Long = todayInstant.getEpochSecond
      // // check if challenge already for today else do nothing..
      // challengeRepo.existByDate(createdAt).map { isCreated =>
      //   if (!isCreated) {
      //     for {
      //       // remove currentChallengeGame and shuffle the result
      //       availableGames <- gameRepo
      //         .all()
      //         .map(games => Random.shuffle(games.filterNot(_.id == SystemSchedulerActor.currentChallengeGame.getOrElse(None))))
      //       // get head, and create new Challenge for the day
      //       _ <- Future.successful {
      //         try {
      //           val game: Game = availableGames.head
      //           val newChallenge = new Challenge(UUID.randomUUID,
      //                                           game.id,
      //                                           "Challenge content is different every day, use your ingenuity to get the first place.",
      //                                           createdAt,
      //                                           expiredAt)

      //           SystemSchedulerActor.currentChallengeGame = Some(game.id)
      //           challengeRepo.add(newChallenge)
      //         } catch {
      //           case e: Throwable => println("Error: No games available")
      //         }
      //       }
      //       // after creating new challenge..
      //       // calculate overe all challenge and save to Challengehistory for tracking top ranks
      //     } yield (self ! ProcessOverAllChallenge(expiredAt))
      //   }
      //   // else self ! ProcessOverAllChallenge(expiredAt)
      // }
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
              // TODO: update user VIP account point
            })
          }
          _ <- Future.successful {
            tracked.map { case DailyTask(user, game_id, game_count) =>
              for {
                vipAcc <- vipUserRepo.findByID(user)
                _ <- Future.successful {
                  if (vipAcc != None) {
                    val vip: VIPUser = vipAcc.get
                    // points (fixed 1 VIP per day) * rank benefit
                    vipUserRepo.getBenefitByID(vip.rank).map {
                      case Some(v) =>
                        // new earned points * redemption rate + prev VIP points
                        val newPoints = vip.points + (v.redemption_rate * 1)
                        // create new updated VIP User
                        vipUserRepo.update(vip.copy(points = newPoints))
                      case None => ()
                    }
                  }
                }
              } yield ()
            }
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

    case RankingScheduler =>
      // get date range to fecth from overall history...
      val start = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).plusDays(-1)
      val end = start.plusDays(1)
      // fetch overAllGameHistory by date ranges
      for {
        gameHistory <- overAllGameHistory.getByDateRange(start.toInstant().getEpochSecond, (end.toInstant().getEpochSecond - 1))
        // grouped by user -> Map[String, Seq[OverAllGameHistory]]
        grouped <- Future.successful(gameHistory.groupBy(_.info.user))
        // grouped history by user..
        processedBets <- Future.successful {
          grouped.map { case (user, histories) =>
            val bets: Seq[(Double, Double)] = histories.map { history =>
              history.info match {
                case GQGameHistory(name, prediction, result, bet) =>
                  if (result) (bet, 1) else (bet, 0)
                case THGameHistory(name, prediction, result, bet, amount) =>
                  if (prediction == result) (bet, amount) else (bet, amount)
                case e => (e.bet, 0)
              }
            }
            (user, bets)
          }
          .toSeq
        }
        profit <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).sum - bets.map(_._1).sum) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankProfit(user.id, v._2, v._3)).getOrElse(null)))
        }
        // total bet amount - total win amount
        payout <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).sum) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankPayout(user.id, v._2, v._3)).getOrElse(null)))
        }
        // total bet amount * (EOS price -> USD)
        wagered <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._1).sum * Config.EOS_TO_USD_CONVERSION) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankWagered(user.id, v._2, v._3)).getOrElse(null)))
        }
        // total win size
        multiplier <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).filter(_ > 0).size) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankMultiplier(user.id, v._2, v._3)).getOrElse(null)))
        }
        // save ranking to history..
        _ <- {
          val rank = RankingHistory(UUID.randomUUID, profit, payout, wagered, multiplier, start.toInstant().getEpochSecond)
          // insert into DB, if failed then re-insert
          Await.ready(rankingHistoryRepo.add(rank), Duration.Inf)
        }
        // update users VIP accounts with new points claimed..
        _ <- Await.ready(processOverallHistoryPointsPerUser(processedBets), Duration.Inf)
      } yield ()

    case _ => ()
  }

  // process overall Game History in 24hrs
  private def processOverallHistoryPointsPerUser(txs: Seq[(String, Seq[(Double, Double)])]): Future[Unit] =
    Future.successful {
      txs.map { case (user, bets) =>
        try {
          for {
            userAcc <- userAccountRepo.getByName(user)
            vipAcc <- vipUserRepo.findByID(userAcc.get.id)
            result <- {
              vipAcc.map { vip =>
                val newTotalPayout = bets.map(_._2).sum + vip.payout
                // create new updated VIP User payout
                vipUserRepo.update(vip.copy(payout = newTotalPayout))
              }
              .getOrElse(Future(1))
            }
          } yield (result)
        } catch {
          case _ : Throwable => ()
        }
      }
    }

  private def processChallengeTrackerAndEarnedVIPPoints(time: Instant): Future[Unit] = {
    for {
      challenges <- challengeTrackerRepo.all()
      // update all users vip account points earned..
      _ <- Future.sequence {
        challenges.map { case ChallengeTracker(user, bets, wagered, ratio, points) =>
          for {
            vipAcc <- vipUserRepo.findByID(user)
            result <- {
              val vip: VIPUser = vipAcc.get
              // new earned points * redemption rate + prev VIP points
              val newPoints = vip.points + points
              // create new updated VIP User
              vipUserRepo.update(vip.copy(points = newPoints))
            }
          } yield (result)
        }
      }
      topHighestWagered <- Future.successful(challenges.sortBy(-_.wagered).take(10))
      addToHistory <- {
        // get all top 10 challenge result
        challengeHistoryRepo
              .add(new ChallengeHistory(UUID.randomUUID, topHighestWagered, time.getEpochSecond))
              .map(x => if (x > 0) challengeTrackerRepo.clearTable else ())
      }
    } yield ()
  }
}