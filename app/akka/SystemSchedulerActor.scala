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
import models.repo. {
  UserAccountWalletHistoryRepo,
  GameRepo,
  ChallengeRepo,
  ChallengeHistoryRepo,
  ChallengeTrackerRepo,
  TaskRepo,
  DailyTaskRepo,
  TaskHistoryRepo,
  OverAllGameHistoryRepo,
  UserAccountRepo,
  RankingHistoryRepo,
  VIPUserRepo
}
import models.service.{ UserAccountService, PlatformConfigService, RankingService }
import models.domain.enum._
import models.domain.wallet.support._
import utils.lib.MultiCurrencyHTTPSupport

object SystemSchedulerActor {
  var currentChallengeGame: Option[UUID] = None
  var isIntialized: Boolean = false
  val walletTransactions = HashMap.empty[String, Event]

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
            rankingService: RankingService,
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
          rankingService,
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
                                    rankingService: RankingService,
                                    vipUserRepo: VIPUserRepo,
                                    httpSupport: MultiCurrencyHTTPSupport,
                                    @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef
                                    )(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
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
          platformConfigService.getGamesInfo().map(_.map(game => WebSocketActor.subscribers.addOne(game.id, self)))
          // if server has stop due to some updates or restart...
          // check if has existing tasks create for today based on UTC
          for {
            isExists <- taskRepo.existByDate(startOfDayUTC())
            updateTask <- Future.successful {
              if (!isExists) self ! CreateNewDailyTask
              else () // do nothing..
            }
          } yield (updateTask)

          // 24hrs Scheduler at 12:00 AM daily
          // any time the system started it will start at 12:AM
          val dailySchedInterval: FiniteDuration = { DEFAULT_SYSTEM_SCHEDULER_TIMER }.hours
          val dailySchedDelay   : FiniteDuration = {
              // val startTime = LocalTime.of(0, 0).toSecondOfDay
              val startTime = 0
              val now = LocalTime.now(defaultTimeZone).toSecondOfDay
              val fullDay = 60 * 60 * DEFAULT_SYSTEM_SCHEDULER_TIMER
              val difference = startTime - now
              if (difference < 0) {
                fullDay + difference
              } else {
                startTime - now
              }
            }.seconds
          system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => {
            self ! ChallengeScheduler
            self ! DailyTaskScheduler
            self ! CreateNewDailyTask
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
                                  userAccountService.deductWalletBalance(account, w.currency, totalAmount)
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
                                      userAccountService.addWalletBalance(account, d.currency, result.value.toDouble)
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
      val time: Instant = dateNowPlusDaysUTC(-1)
      // update all earned points into users Account
      for {
        challenges <- challengeTrackerRepo.all()
        topHighestWagered <- Future.successful(challenges.sortBy(-_.wagered).take(10))
        // process Ranking on the other thread,,
        _ <- Future.successful(self ! RankingScheduler(topHighestWagered))
        addToHistory <- {
          // get all top 10 challenge result
          for {
            isAdded <- challengeHistoryRepo.add(new ChallengeHistory(UUID.randomUUID, topHighestWagered, time.getEpochSecond))
            done <- if (isAdded > 0) challengeTrackerRepo.clearTable else Future.successful(0)
          } yield (done)
        }
      } yield (addToHistory)
    // Daily tasks rewards are fixed amount based on time of tasks reward generation
    case DailyTaskScheduler =>
      taskRepo.getTaskWithOffset(0).map(_.map { v =>
        for {
          tracked <- dailyTaskRepo.all()
          // update users vip points from daily tasks
          processes <- Future.sequence {
            tracked.map { x =>
              for {
                vipAcc <- vipUserRepo.findByID(x.user)
                updatedVIPAcc <- {
                  vipAcc
                    .map { vip =>
                      for {
                        hasBnft <- vipUserRepo.getBenefitByID(vip.rank)
                        updateAccount <- {
                          hasBnft.map { bnft =>
                            val isTaskExists: Option[TaskGameInfo] = v.tasks.find(_.game.id == x.game_id)
                            // user play count >= task play count
                            isTaskExists
                              .map(task => vipUserRepo.update(vip.copy(points = vip.points + task.points)))
                              .getOrElse(Future.successful(0))

                          }
                          .getOrElse(Future.successful(0))
                        }
                      } yield (updateAccount)
                    }
                    .getOrElse(Future.successful(0))
                }
              } yield (updatedVIPAcc)
            }
          }
          // insert all data into database history
          insertHistory <- {
            val time: Instant = dateNowPlusDaysUTC(-1)
            val taskHistory = new TaskHistory(v.id,
                                              tracked.toList,
                                              time,
                                              Instant.ofEpochSecond(time.getEpochSecond + ((60 * 60 * 24) - 1)))
            taskHistoryRepo.add(taskHistory)
          }
          clean <- if (insertHistory > 0) dailyTaskRepo.clearTable else Future.successful(0)
        } yield (insertHistory)
      })


    case CreateNewDailyTask =>
      val time: Long = startOfDayUTC().getEpochSecond
      // val startOfDay: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).toInstant()
      taskRepo.existByDate(Instant.ofEpochSecond(time)).map { isCreated =>
        if (!isCreated) {
          for {
            tasks <- createRandomTasks()
            updateTask <- taskRepo.add(new Task(UUID.randomUUID, tasks, time))
          } yield (updateTask)
        }
      }

    case RankingScheduler(data) =>
      for {
        rankedData <- rankingService.calculateRank(data)
        // save ranking to history..
        added <- rankingHistoryRepo.add(rankedData)
        // update users VIP accounts with after rankAdded
        clean <- if (added > 0) claimPointsPerUser(data) else Future.successful(0)
      } yield (clean)

    case _ => ()
  }

  // process overall Game History in 24hrs
  private def claimPointsPerUser(data: Seq[ChallengeTracker]): Future[Seq[Int]] =
    Future.sequence {
      data.map { case ChallengeTracker(id, bets, wagered, ratio, points, payout, multiplier) =>
        for {
          vipAcc <- vipUserRepo.findByID(id)
          updatePoints <- {
            vipAcc.map { vip =>
              // find VIP account
              for {
                hasBnft <- vipUserRepo.getBenefitByID(vip.rank)
                update <- {
                  hasBnft
                    .map { bnft =>
                      // multiply earnings with account vip multiplier + old point
                      val newTotalPayout = vip.payout + (payout * bnft.redemption_rate)
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

  private def createRandomTasks(): Future[List[TaskGameInfo]] = {
    for {
      availableGames <- gameRepo.all()
      tasks <- Future.successful {
        // generate random range from 1 - 5
        // to determine how many games need to play to get points
        if (availableGames.isEmpty)
          initialGames.map(x => TaskGameInfo(x, Random.between(1, 6), roundAt(2)(Random.between(0, 2.0)), None))
        else
          availableGames.map(x => TaskGameInfo(x, Random.between(1, 6), roundAt(2)(Random.between(0, 2.0)), None))
      }
    } yield (tasks)
  }
}