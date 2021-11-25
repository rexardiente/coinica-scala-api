package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ UUID, Calendar }
import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, ZoneOffset, ZoneId, ZonedDateTime }
import scala.util.{ Success, Failure, Random }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import Ordering.Double.IeeeOrdering
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import akka.util.Timeout
import utils.{GameConfig, SystemConfig }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import akka.common.objects._
import models.domain._
import models.repo._
import models.service.UserAccountService
import models.domain.enum._
import models.domain.wallet.support._
import utils.lib.MultiCurrencyHTTPSupport

object SystemSchedulerActor {
  var currentChallengeGame: Option[UUID] = None
  var isIntialized: Boolean = false
  val walletTransactions = HashMap.empty[String, Event]
  def props(
            userAccountService: UserAccountService,
            userWalletRepo: UserAccountWalletHistoryRepo,
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
          userWalletRepo,
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
case object WalletTxScheduler

@Singleton
class SystemSchedulerActor @Inject()(userAccountService: UserAccountService,
                                    userWalletRepo: UserAccountWalletHistoryRepo,
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

  private def roundAt(p: Int)(n: Double): Double = { val s = math pow (10, p); (math round n * s) / s }

  override def preStart: Unit = {
    super.preStart

    QuartzSchedulerExtension(system).schedule("WalletTxScheduler", self, WalletTxScheduler)
    // keep alive connection
    // https://stackoverflow.com/questions/13700452/scheduling-a-task-at-a-fixed-time-of-the-day-with-akka
    akka.stream.scaladsl.Source.tick(0.seconds, 20.seconds, "SystemSchedulerActor").runForeach(n => ())
    // check if intializer is the SchedulerActor module..
    system.actorSelection("/user/SystemSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!SystemSchedulerActor.isIntialized) {
          // 24hrs Scheduler at 12:00 AM daily
          // any time the system started it will start at 12:AM
          val dailySchedInterval: FiniteDuration = { SystemConfig.DEFAULT_SYSTEM_SCHEDULER_TIMER }.hours
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
          WebSocketActor.subscribers.addOne(GameConfig.GQ_GAME_ID, self)
          WebSocketActor.subscribers.addOne(GameConfig.TH_GAME_ID, self)
          WebSocketActor.subscribers.addOne(GameConfig.MJHilo_GAME_ID, self)
          log.info("System Scheduler Actor Initialized")
        }
      case Failure(ex) =>  ()// if actor is already created do nothing..
    }
  }

  def receive: Receive = {
    // ETH and USDC wallet tx details checker,
    // limit checking of tx details failed..
    case WalletTxScheduler =>
      SystemSchedulerActor.walletTransactions.map { data =>
        val txHash: String = data._1
        for {
          // check if txhash already exists else do nothing,.
          isTxHashExists <- userWalletRepo.existByTxHash(txHash)
          processWithdrawOrDeposit <- {
            if (!isTxHashExists) {
              // validate instance of data/Event object..
              data._2 match {
                case w: ETHUSDCWithdrawEvent =>
                  for {
                    wallet <- userAccountService.getUserAccountWallet(w.account_id.getOrElse(UUID.randomUUID))
                    processWithdraw <- {
                      wallet.map { account =>
                        w.currency match {
                          case "USDC" | "ETH" =>
                            for {
                              txDetails <- httpSupport.getETHTxInfo(txHash, w.currency)
                              // update DB and history..
                              updateBalance <- {
                                txDetails.map { detail =>
                                  val initialAmount: Double = detail.result.value.toDouble
                                  val totalAmount: BigDecimal = w.currency match {
                                    case "USDC" =>
                                      (detail.result.gasPrice * SystemConfig.DEFAULT_WEI_VALUE) + initialAmount

                                    case "ETH" =>
                                      val maxTxFeeLimit: BigDecimal = 500000
                                      (((maxTxFeeLimit * detail.result.gasPrice) * SystemConfig.DEFAULT_WEI_VALUE) + initialAmount)
                                  }
                                  userAccountService.deductBalanceByCurrency(account.id, w.currency, totalAmount)
                                }
                                .getOrElse(Future(0))
                              }

                              addHistory <- {
                                if (updateBalance > 0) {
                                  // save to history
                                  userAccountService.saveUserWalletHistory(
                                    new UserAccountWalletHistory(txHash,
                                                                account.id,
                                                                w.currency,
                                                                w.tx_type,
                                                                txDetails.map(_.result).getOrElse(null),
                                                                Instant.now))
                                    .map { isAdded =>
                                      if (isAdded > 0) {
                                        // send user a notification process sucessful
                                        WebSocketActor.subscribers(account.id) !
                                        OutEvent(JsString("DEPOSIT_WITHDRAW_EVENT"),
                                                (txDetails.get.toJson.as[JsObject] + ("tx_type" -> JsString(w.tx_type))))
                                        (1)
                                      } else (0)
                                    }
                                }
                                else Future(0)
                              }
                            } yield (addHistory)
                          case _ => Future(0)
                        }
                      }.getOrElse(Future(0))
                    }
                  } yield (processWithdraw)

                case d: DepositEvent =>
                  for {
                    wallet <- userAccountService.getUserAccountWallet(d.account_id.getOrElse(UUID.randomUUID))
                    processDeposit <- {
                      wallet.map { account =>
                        d.currency match {
                          case "USDC" | "ETH" =>
                            for {
                              txDetails <- httpSupport.getETHTxInfo(txHash, d.currency)
                              // update DB and history..
                              updateBalance <- {
                                txDetails.map { detail =>
                                    val result: ETHJsonRpcResult = detail.result

                                    if (result.from == d.issuer && result.to == d.receiver) {
                                      userAccountService.addBalanceByCurrency(account.id, d.currency, result.value.toDouble)
                                    }
                                    else Future(0)
                                }
                                .getOrElse(Future(0))
                              }

                              addHistory <- {
                                if (updateBalance > 0) {
                                  // save to history
                                  userAccountService.saveUserWalletHistory(
                                    new UserAccountWalletHistory(txHash,
                                                                account.id,
                                                                d.currency,
                                                                d.tx_type,
                                                                txDetails.map(_.result).getOrElse(null),
                                                                Instant.now))
                                    .map { isAdded =>
                                      if (isAdded > 0) {
                                        // send user a notification process sucessful
                                        WebSocketActor.subscribers(account.id) !
                                        OutEvent(JsString("DEPOSIT_WITHDRAW_EVENT"),
                                                          (txDetails.get.toJson.as[JsObject] + ("tx_type" -> JsString(d.tx_type))))
                                        (1)
                                      } else (0)
                                    }
                                }
                                else Future(0)
                              }
                            } yield (addHistory)
                          case _ => Future(0)
                        }
                      }.getOrElse(Future(0))
                    }
                  } yield (processDeposit)
                case _ => Future(0) // do nothing
              }
            }
            else Future(0)
          }
          // if already exists on DB, remove tx from the list..
          _ <- Future.successful {
            if (isTxHashExists) SystemSchedulerActor.walletTransactions.remove(txHash)
            if (processWithdrawOrDeposit > 0) SystemSchedulerActor.walletTransactions.remove(txHash)
          }
        } yield ()
      }

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
    // Daily tasks rewards are fixed amount based on time of tasks reward generation
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
              // taskHistoryRepo.add(taskHistory).map(isAdded => if(isAdded > 0)() else trackedFailedInsertion.addOne(taskHistory) )
              taskHistoryRepo
                .add(taskHistory)
                .map { isAdded =>
                  //  update vip points after added into history..
                  if(isAdded > 0) {
                    for {
                      vipAcc <- vipUserRepo.findByID(x.user)
                      updatedVIPAcc <- Future.successful {
                        vipAcc.map { vip =>
                          // points (fixed 1 VIP per day) * rank benefit
                          vipUserRepo.getBenefitByID(vip.rank).map {
                            case Some(b) =>
                              val isTaskExists: Option[TaskGameInfo] = v.tasks.filter(_.game.id == x.game_id).headOption
                              // user play count >= task play count
                              isTaskExists.map { gameInfo =>
                                // update User VIP points
                                if (x.game_count >= gameInfo.count) {
                                  for {
                                    _ <- vipUserRepo.update(vip.copy(points = vip.points + gameInfo.points))
                                    _ <- isUpdateToNewVIPLvl(vip)
                                  } yield ()
                                }
                                else () // do nothing..
                              }

                            case None => ()
                          }
                        }
                        .getOrElse(Future.successful(0))
                      }
                    } yield (updatedVIPAcc)
                  }
                  else trackedFailedInsertion.addOne(taskHistory)
                }
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
            availableGames <- gameRepo.all()
            _ <- Future.successful {
              try {
                // generate random range from 1 - 5
                // to determine how many games need to play to get points
                val tasks: Seq[TaskGameInfo] = availableGames.map(x => TaskGameInfo(x, Random.between(1, 6), roundAt(2)(Random.between(0, 2.0)), None))
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
            // val bets: Seq[(Double, Double)] = histories.map { history =>
            //   history.info match {
            //     case BooleanPredictions(name, prediction, result, bet, amount) =>
            //       if (prediction == result) (bet, amount) else (bet, amount)
            //     case ListOfIntPredictions(name, prediction, result, bet, amount) =>
            //       if (prediction == result) (bet, amount) else (bet, amount)
            //     case e => (e.bet, 0)
            //   }
            // }
            (user, histories.map(x => (x.info.bet, x.info.amount)))
          }
          .toSeq
        }
        profit <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).sum - bets.map(_._1).sum) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankProfit(user.id, user.username, v._2, v._3)).getOrElse(null)))
        }
        // total bet amount - total win amount
        payout <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).sum) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankPayout(user.id, user.username, v._2, v._3)).getOrElse(null)))
        }
        currentUSDValue <- httpSupport.getCurrentPriceBasedOnMainCurrency(SystemConfig.SUPPORTED_SYMBOLS(0))
        // total bet amount * (EOS price -> USD)
        wagered <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._1).sum * currentUSDValue.toDouble) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankWagered(user.id, user.username, v._2, v._3)).getOrElse(null)))
        }
        // total win size
        multiplier <- Future.sequence {
          processedBets
            .map { case (user, bets) => (user, bets.map(_._1).sum, bets.map(_._2).filter(_ > 0).size) }
            .sortBy(-_._3)
            .take(10)
            .filter(_._3 > 0)
            .map(v => userAccountRepo.getByName(v._1).map(_.map(user => RankMultiplier(user.id, user.username, v._2, v._3)).getOrElse(null)))
        }
        // save ranking to history..
        _ <- {
          val rank: RankingHistory = RankingHistory(profit, payout, wagered, multiplier, start.toInstant().getEpochSecond)
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
              vipAcc
                .map { acc =>
                  val newPoints = acc.points + points
                  // create new updated VIP User
                  vipUserRepo.update(acc.copy(points = newPoints))
                }
                .getOrElse(Future.successful(0))
            }
            updateLvl <- vipAcc.map(isUpdateToNewVIPLvl).getOrElse(Future.successful(0))
          } yield (updateLvl)
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

  private def isUpdateToNewVIPLvl(vip: VIPUser): Future[Boolean] = {
    // check if account has enough points for next lvlup
    if (vip.currentRank == vip.rank) Future.successful(false) // do nothing
    // update VIP rank details
    else {
      vipUserRepo
        .update(vip.copy(rank = vip.currentRank, next_rank = vip.nextRank()))
        .map { isUpdated => if (isUpdated > 0) true else false }
    }
  }
}