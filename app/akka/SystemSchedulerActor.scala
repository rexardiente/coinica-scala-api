package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ UUID, Calendar }
import java.time.{ Instant, LocalTime, LocalDate }
import scala.util.{ Success, Failure, Random }
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import Ordering.Double.IeeeOrdering
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import akka.util.Timeout
import utils.SystemConfig._
import play.api.libs.ws.WSClient
import play.api.libs.json._
import akka.common.objects._
import models.domain._
import models.repo._
import models.service.{ UserAccountService, PlatformConfigService }
import models.domain.enum._
import models.domain.wallet.support._
import utils.lib.MultiCurrencyHTTPSupport

object SystemSchedulerActor {
  var currentChallengeGame: Option[UUID] = None
  var isIntialized: Boolean = false
  val walletTransactions = HashMap.empty[String, Event]
  // game objects here..
  var ghostquest: Option[PlatformGame] = None
  var mahjonghilo: Option[PlatformGame] = None
  var treasurehunt: Option[PlatformGame] = None

  def props(platformConfigService: PlatformConfigService,
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
          platformConfigService,
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
class SystemSchedulerActor @Inject()(platformConfigService: PlatformConfigService,
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
                                    @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private def defaultScheduler: Int = DEFAULT_SYSTEM_SCHEDULER_TIMER
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
          // init notification actor for every games..
          SystemSchedulerActor.ghostquest.map(game => WebSocketActor.subscribers.addOne(game.id, self))
          SystemSchedulerActor.mahjonghilo.map(game => WebSocketActor.subscribers.addOne(game.id, self))
          SystemSchedulerActor.treasurehunt.map(game => WebSocketActor.subscribers.addOne(game.id, self))
          // if server has stop due to some updates or restart...
          // check if has existing tasks create for today based on UTC
          for {
            isExists <- taskRepo.existByDate(startOfDayUTC())
            randomTasks <- createRandomTasks()
            updateTask <- {
              if (!isExists) taskRepo.add(new Task(UUID.randomUUID, randomTasks, startOfDayUTC().getEpochSecond))
              else Future.successful(0) // do nothing..
            }
          } yield (updateTask)

          // 24hrs Scheduler at 12:00 AM daily
          // any time the system started it will start at 12:AM
          val dailySchedInterval: FiniteDuration = { defaultScheduler }.hours
          val dailySchedDelay   : FiniteDuration = {
              val startTime = LocalTime.of(0, 0).toSecondOfDay
              val now = LocalTime.now(defaultTimeZone).toSecondOfDay
              val fullDay = 60 * 60 * 24
              val difference = startTime - now
              if (difference < 0) {
                fullDay + difference
              } else {
                startTime - now
              }
            }.seconds
          system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => {
            // reload config with scheduler to apply changes in config table
            loadDefaultObjects()
            // block runners to make sure configs are laoded..
            Thread.sleep(1000)
            self ! ChallengeScheduler
            self ! DailyTaskScheduler
            self ! RankingScheduler
          })
          // set true if actor already initialized
          SystemSchedulerActor.isIntialized = true
          log.info("System Scheduler Actor Initialized")
        }
      case Failure(ex) =>  ()// if actor is already created do nothing..
    }
  }
  override def postStop: Unit = {
    super.postStop
    println("Stop all threads on SystemSchedulerActor")
    system.stop(self)
  }

  private def loadDefaultObjects() = {
    for {
      // load ghostquest game defaults..
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("ghostquest")
          .map(game => { SystemSchedulerActor.ghostquest = game })
      }
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("mahjonghilo")
          .map(game => { SystemSchedulerActor.mahjonghilo = game })
      }
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("treasurehunt")
          .map(game => { SystemSchedulerActor.treasurehunt = game })
      }
    } yield ()
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
                                      (detail.result.gasPrice * DEFAULT_WEI_VALUE) + initialAmount

                                    case "ETH" =>
                                      val maxTxFeeLimit: BigDecimal = 500000
                                      (((maxTxFeeLimit * detail.result.gasPrice) * DEFAULT_WEI_VALUE) + initialAmount)
                                  }
                                  userAccountService.deductBalanceByCurrency(account, w.currency, totalAmount)
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
                                                                instantNowUTC()))
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
                                      userAccountService.addBalanceByCurrency(account, d.currency, result.value.toDouble)
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
                                                                instantNowUTC()))
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
      val yesterday: Instant = dateNowPlusDaysUTC(-1)
      // update all earned points into users Account
      Await.ready(processChallengeTrackerAndEarnedVIPPoints(yesterday), Duration.Inf)
    // Daily tasks rewards are fixed amount based on time of tasks reward generation
    case DailyTaskScheduler =>
      val yesterday: Instant = dateNowPlusDaysUTC(-1)
      // process first all available task before creating new tasks
      val trackedFailedInsertion = ListBuffer.empty[TaskHistory]
      val aPromise = Promise[Future[Int]]()
      val processedTasks: Future[Int] = taskRepo.getDailyTaskByDate(yesterday).map(_.map { v =>
        for {
          tracked <- dailyTaskRepo.all()
          // update users vip points from daily tasks
          processes <- Future.sequence {
            tracked.map { x =>
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
          }
          // insert all data into database history
          insertHistory <- {
            val taskHistory = new TaskHistory(UUID.randomUUID,
                                              tracked.toList,
                                              yesterday,
                                              Instant.ofEpochSecond(yesterday.getEpochSecond + ((60 * 60 * 24) - 1)))
            taskHistoryRepo.add(taskHistory)
          }
          clean <- dailyTaskRepo.clearTable
        } yield (clean)
      }
      .getOrElse(Future.successful(0)))
      .flatten
      // wait the process to be completed
      aPromise.success(processedTasks)
      // get the future result from promise action
      // flatten to convert Future[Future[]] to Future[]
      val completedTasks: Future[Int] = aPromise.future.flatten
      // create new tasks for another day
      completedTasks.map { _ => self ! (CreateNewDailyTask, yesterday) }

    case (CreateNewDailyTask, yesterday: Instant) =>
      val startOfDay: Long = yesterday.getEpochSecond + (60 * 60 * 24)
      // val startOfDay: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).toInstant()
      taskRepo.existByDate(Instant.ofEpochSecond(startOfDay)).map { isCreated =>
        if (!isCreated) {
          for {
            availableGames <- gameRepo.all()
            tasks <- createRandomTasks()
            updateTask <- taskRepo.add(new Task(UUID.randomUUID, tasks, startOfDay))
          } yield (updateTask)
        }
      }

    case RankingScheduler =>
      // get date range to fecth from overall history...
      val timeZone = LocalDate.now(defaultTimeZone).atStartOfDay().plusDays(-1)
      val start: Long = timeZone.toInstant(defaultTimeZone).getEpochSecond
      val end: Long = timeZone.plusDays(1).toInstant(defaultTimeZone).getEpochSecond
      // fetch overAllGameHistory by date ranges
      for {
        gameHistory <- overAllGameHistory.getByDateRange(start, (end - 1))
        // grouped by user -> Map[String, Seq[OverAllGameHistory]]
        grouped <- Future.successful(gameHistory.groupBy(_.info.user))
        // grouped history by user..
        processedHistory <- Future.successful {
          grouped
            .map { case (user, histories) =>
              val amountHistories: List[(Double, Double)] = histories.map(x => (x.info.bet, x.info.amount)).toList
              // calculate total bet, earn and multiplier
              // amounts are converted into USD
              val bets: Double = amountHistories.map(_._1).sum
              val earnings: Double = amountHistories.map(_._2).sum
              val multipliers: Int = amountHistories.map(_._2).filter(_ > 0).size
              (user, bets, earnings, multipliers)
            }.toSeq
        }
        profit <- Future.sequence {
          processedHistory
            .map { case (user, bets, earnings, multipliers) => (user, bets, earnings - bets) }
            .filter(_._3 > 0)
            .sortBy(-_._3)
            .take(10)
            .map { case (user, bets, profit) =>
              userAccountRepo
                .getByName(user)
                .map(_.map(account => RankProfit(account.id, account.username, bets, profit)))
            }
        }
        payout <- Future.sequence {
          processedHistory
              .map { case (user, bets, earnings, multipliers) => (user, bets, earnings) }
              .filter(_._3 > 0)
              .sortBy(-_._3)
              .take(10)
              .map { case (user, bets, payout) =>
                userAccountRepo
                  .getByName(user)
                  .map(_.map(account => RankPayout(account.id, account.username, bets, payout)))
              }
        }
        wagered <- Future.sequence {
          processedHistory
              .map { case (user, bets, earnings, multipliers) => (user, bets, bets) }
              .filter(_._3 > 0)
              .sortBy(-_._3)
              .take(10)
              .map { case (user, bets, wagered) =>
                userAccountRepo
                  .getByName(user)
                  .map(_.map(account => RankWagered(account.id, account.username, bets, wagered)))
              }
        }
        // total win size
        multiplier <- Future.sequence {
          processedHistory
              .map { case (user, bets, earnings, multipliers) => (user, bets, multipliers) }
              .filter(_._3 > 0)
              .sortBy(-_._3)
              .take(10)
              .map { case (user, bets, multipliers) =>
                userAccountRepo
                  .getByName(user)
                  .map(_.map(account => RankMultiplier(account.id, account.username, bets, multipliers)))
              }
        }
        // save ranking to history..
        rankAdded <- {
          //  remove null values from the list..
          val aProfit: Seq[RankType] = removeNoneValue[RankType](profit)
          val aPayout: Seq[RankType] = removeNoneValue[RankType](payout)
          val aWagered: Seq[RankType] = removeNoneValue[RankType](wagered)
          val aMultiplier: Seq[RankType] = removeNoneValue[RankType](multiplier)
          val rank: RankingHistory = RankingHistory(aProfit, aPayout, aWagered, aMultiplier, start)
          // insert into DB, if failed then re-insert
          rankingHistoryRepo.add(rank)
        }
        // update users VIP accounts with after rankAdded
        claimPoints <- claimPointsPerUser(processedHistory)
      } yield (claimPoints)

    case _ => ()
  }
  // remove null values from list[RankType]
  private def removeNoneValue[T >: RankType](v: Seq[Option[T]]): Seq[T] = v.map(_.getOrElse(null))
  // process overall Game History in 24hrs
  private def claimPointsPerUser(txs: Seq[(String, Double, Double, Int)]): Future[Seq[Int]] =
    Future.sequence {
      txs.map { case (user, bets, earnings, multipliers) =>
        for {
          userAcc <- userAccountRepo.getByName(user)
          vipAcc <- vipUserRepo.findByID(userAcc.get.id)
          updatePoints <- {
            vipAcc.map { vip =>
              // find VIP account
              for {
                hasBnft <- vipUserRepo.getBenefitByID(vip.rank)
                update <- {
                  hasBnft
                    .map { bnft =>
                      // multiply earnings with account vip multiplier + old point
                      val newTotalPayout = vip.payout + (earnings * bnft.redemption_rate)
                      // update VIP User payout
                      vipUserRepo.update(vip.copy(payout = newTotalPayout))
                    }
                    .getOrElse(Future.successful(0))
                }
              } yield (update)
            }
            .getOrElse(Future(0))
          }
        } yield (updatePoints)
      }
    }

  private def processChallengeTrackerAndEarnedVIPPoints(time: Instant): Future[Int] = {
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
        for {
          isAdded <- challengeHistoryRepo.add(new ChallengeHistory(UUID.randomUUID, topHighestWagered, time.getEpochSecond))
          done <- if (isAdded > 0) challengeTrackerRepo.clearTable else Future.successful(0)
        } yield (done)
      }
    } yield (addToHistory)
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

  private def createRandomTasks(): Future[List[TaskGameInfo]] = {
    for {
      availableGames <- gameRepo.all()
      tasks <- Future.successful {
        // generate random range from 1 - 5
        // to determine how many games need to play to get points
        availableGames.map(x => TaskGameInfo(x, Random.between(1, 6), roundAt(2)(Random.between(0, 2.0)), None))
      }
    } yield (tasks)
  }
}