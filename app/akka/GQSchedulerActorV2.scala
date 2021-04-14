package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset, ZoneId }
import scala.util.{ Success, Failure }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import play.api.libs.ws.WSClient
import models.domain._
import models.domain.eosio.{ TableRowsRequest, GQCharacterGameHistory, GQBattleResult }
import models.repo.{ OverAllGameHistoryRepo, DailyTaskRepo, TaskRepo, UserAccountRepo }
import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
import models.service.GQSmartContractAPI
import utils.lib.{ EOSIOSupport, GQBattleCalculation }
import models.domain.eosio.GQ.v2._
import akka.common.objects._
import utils.Config

object GQSchedulerActorV2 {
  val defaultTimeSet: Int            = Config.GQ_DEFAULT_BATTLE_TIMER
  val scheduledTime : FiniteDuration = { defaultTimeSet }.minutes
  var isIntialized  : Boolean        = false
  val EOSTable: TableRowsRequest = new TableRowsRequest(
                                        Config.GQ_CODE,
                                        Config.GQ_TABLE,
                                        Config.GQ_SCOPE,
                                        None,
                                        Some("uint64_t"),
                                        None,
                                        None,
                                        None)
  def props(characterRepo: GQCharacterDataRepo,
            historyRepo: GQCharacterGameHistoryRepo,
            gameTxHistory: OverAllGameHistoryRepo,
            accountRepo: UserAccountRepo,
            taskRepo: TaskRepo,
            dailyTaskRepo: DailyTaskRepo,
            eosioHTTPSupport: EOSIOHTTPSupport)(implicit system: ActorSystem) =
    Props(classOf[GQSchedulerActorV2],
          characterRepo,
          historyRepo,
          gameTxHistory,
          accountRepo,
          taskRepo,
          dailyTaskRepo,
          eosioHTTPSupport,
          system)
}

@Singleton
class GQSchedulerActorV2 @Inject()(
      characterRepo: GQCharacterDataRepo,
      gQGameHistoryRepo: GQCharacterGameHistoryRepo,
      gameTxHistory: OverAllGameHistoryRepo,
      accountRepo: UserAccountRepo,
      taskRepo: TaskRepo,
      dailyTaskRepo: DailyTaskRepo,
      eosioHTTPSupport: EOSIOHTTPSupport,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
    )(implicit system: ActorSystem ) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

  override def preStart: Unit = {
    super.preStart
    // keep alive connection
    akka.stream.scaladsl.Source.tick(0.seconds, 60.seconds, "GQSchedulerActorV2").runForeach(n => ())
    system.actorSelection("/user/GQSchedulerActorV2").resolveOne().onComplete {
      case Success(actor) =>
        if (!GQSchedulerActorV2.isIntialized) {
          GQSchedulerActorV2.isIntialized = true

          GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTimeSet)
          systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
          log.info("GQ Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    case REQUEST_BATTLE_NOW =>
      try {
        // get all characters on SC and store in GQBattleScheduler.characters
        Await.ready(getEOSTableRows(Some("REQUEST_ON_BATTLE")), Duration.Inf)
        Thread.sleep(1000)
        // filter characters based on condition and battle all available characters
        for {
          filtered <- Await.ready(removeEliminatedAndWithdrawn(GQBattleScheduler.characters), Duration.Inf)
          _ <- Await.ready(battleProcess(filtered), Duration.Inf)
          // broadcast characters no available enemy..
          _ <- Future.successful {
            dynamicBroadcast ! ("BROADCAST_CHARACTER_NO_ENEMY", GQBattleScheduler.noEnemy.groupBy(_._2))
          }
        } yield ()
        Thread.sleep(5000)

        // cleanup memory by removing tracked data
        GQBattleScheduler.noEnemy.clear
        GQBattleScheduler.characters.clear

        if (GQBattleScheduler.battleCounter.isEmpty) {
          dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
          GQBattleScheduler.battleCounter.clear
          defaultSchedule()
        }
        else {
          val battleCounter = HashMap.empty[String, (UUID, GQBattleResult)]

          GQBattleScheduler.battleCounter.map { counter =>
            val winner = counter._2.characters.filter(_._2._2).head
            val loser = counter._2.characters.filter(!_._2._2).head

            for {
              p1 <- accountRepo.getByID(winner._2._1)
              p2 <- accountRepo.getByID(loser._2._1)
              _ <- Await.ready({
                eosioHTTPSupport.battleResult(counter._1.toString, (winner._1, p1.map(_.name).getOrElse("")), (loser._1, p2.map(_.name).getOrElse(""))).map {
                  case Some(e) => battleCounter.addOne(e, counter)
                  case e => null
                }
              }, Duration.Inf)
            } yield ()
          }
          Thread.sleep(1000)

          if (!battleCounter.isEmpty) {
            for {
              _ <- saveToGameHistory(battleCounter.toSeq)
              _ <- insertOrUpdateSystemProcess(battleCounter.toSeq) // update system process
            } yield (GQBattleScheduler.battleCounter.clear)
            // fetch SC table rows and save into GQBattleScheduler.eliminatedOrWithdrawn
            Await.ready(getEOSTableRows(Some("REQUEST_REMOVE_NO_LIFE")), Duration.Inf)
            Thread.sleep(1000)
            // remove no life characters
            // successfully removed characters in SC must be removed from DB
            Await.ready(removedNoLifeContract(), Duration.Inf)
            Thread.sleep(1000)

            Await.ready(for {
              // check if theres existing chracters that has no life
              hasNoLife <- characterRepo.getNoLifeCharacters
              mergeSeq <- Future.successful(characterRepo.mergeSeq[GQCharacterData, Seq[GQCharacterData]](GQBattleScheduler.toRemovedCharacters.map(_._2).toSeq, hasNoLife))
              _ <- Future.successful {
                if (!mergeSeq.isEmpty) {
                  Await.ready(Future.successful(mergeSeq.map { data =>
                                for {
                                  isRemoved <- characterRepo.remove(data.owner, data.key)
                                  _ <- {
                                    if (isRemoved > 0)
                                      characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
                                    else
                                      Future(0) // TODO: re-try if failed tx.
                                  }
                                } yield (Thread.sleep(1000))
                              }), Duration.Inf)
                  Thread.sleep(2000)
                  // broadcast users that DB has been update..
                  // update again overall DB to make sure its updated..
                  Await.ready(getEOSTableRows(Some("REQUEST_UPDATE_CHARACTERS_DB")), Duration.Inf)
                  Thread.sleep(2000)

                  Await.ready(Future.successful(GQBattleScheduler.isUpdatedCharacters
                                  .map(_._2)
                                  .toSeq
                                  .map(characterRepo.updateOrInsertAsSeq(_))), Duration.Inf)
                  Thread.sleep(1000)
                  dynamicBroadcast ! "BROADCAST_DB_UPDATED"
                }
              }
            } yield (), Duration.Inf)
            Thread.sleep(1000)

            battleCounter.clear
            GQBattleScheduler.toRemovedCharacters.clear
            GQBattleScheduler.isUpdatedCharacters.clear
            GQBattleScheduler.eliminatedOrWithdrawn.clear
            defaultSchedule()
          }
          else {
            dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
            defaultSchedule()
          }
        }
      } catch {
        case _: Throwable => // reset all tracker into default..
          GQBattleScheduler.noEnemy.clear
          GQBattleScheduler.toRemovedCharacters.clear
          GQBattleScheduler.characters.clear
          GQBattleScheduler.battleCounter.clear
          GQBattleScheduler.isUpdatedCharacters.clear
          GQBattleScheduler.eliminatedOrWithdrawn.clear
          defaultSchedule()
      }

    case _ => ()
  }

  private def removedNoLifeContract(): Future[Any] = {
    try {
      // filter only characters that has no more life..
      val eliminatedOrWithdrawn = GQBattleScheduler.eliminatedOrWithdrawn.filter(x => x._2.life < 1)
      // remove from smartcontract
      Future.sequence(eliminatedOrWithdrawn.map {
        case (id, data) =>
          for {
            account <- accountRepo.getByID(data.owner)
            result <- Future.successful {
              if (account != None) {
                val acc: UserAccount =  account.get

                eosioHTTPSupport.eliminate(acc.name, id).map {
                  case Some(txHash) => GQBattleScheduler.toRemovedCharacters.addOne(id, data)
                  case None => ()
                }
              }
            }
          } yield (result)
      })
    } catch { case _ : Throwable => Future(()) }
  }
  // challengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double)
  private def insertOrUpdateSystemProcess(seq: Seq[(String, (UUID, GQBattleResult))]): Future[Seq[Unit]] = Future.successful {
    seq.map { case (hash, (gameID, result)) =>
      result.characters.map { v =>
        dynamicProcessor ! DailyTask(v._2._1, Config.GQ_GAME_ID, 1)
        dynamicProcessor ! ChallengeTracker(v._2._1, 1, (if(v._2._2) 2 else 0), 1, (if(v._2._2) 1 else 0))
      }
    }
  }

  private def saveToGameHistory(data: Seq[(String, (UUID, GQBattleResult))]): Future[Seq[Any]] = Future.sequence {
    data.map { case (txHash, (gameID, result)) =>
      val winner = result.characters.filter(_._2._2).head
      val loser = result.characters.filter(!_._2._2).head
      val time = Instant.now.getEpochSecond

      Await.ready(
        for {
          winnerAcc <- accountRepo.getByID(winner._2._1)
          loserAcc <- accountRepo.getByID(loser._2._1)
        } yield ((new OverAllGameHistory(
                            UUID.randomUUID,
                              txHash,
                              gameID.toString,
                              Config.GQ_CODE,
                              GQGameHistory(winnerAcc.map(_.name).getOrElse("no_user"), "WIN", true),
                              true,
                              time),
          new OverAllGameHistory(
                              UUID.randomUUID,
                              txHash,
                              gameID.toString,
                              Config.GQ_CODE,
                              GQGameHistory(loserAcc.map(_.name).getOrElse("no_user"), "WIN", false),
                              true,
                              time)),
          new GQCharacterGameHistory(
                        gameID.toString, // Game ID
                        txHash,
                        winner._2._1,
                        winner._1,
                        loser._2._1,
                        loser._1,
                        result.logs,
                        time)), Duration.Inf)
          .andThen {
            case Success(((winner, loser), character)) =>
              // insert Tx and character contineously
              // broadcast game result to connected users
              // use live data to feed on history update..
              Await.ready(for {
                _ <- gQGameHistoryRepo.insert(character)
                _ <- gameTxHistory.add(winner)
                _ <- gameTxHistory.add(loser)
              } yield (Thread.sleep(1000)), Duration.Inf)
              // broadcast GQ game result
              dynamicBroadcast ! Array(winner, loser)
            case Failure(e) => ()
          }
    }
  }
  // make sure no eliminated or withdrawn characters on the list
  // and shuflle remaining characters..
  private def removeEliminatedAndWithdrawn(v: HashMap[String, GQCharacterData]): Future[HashMap[String, GQCharacterData]] = {
    val characters: HashMap[String, GQCharacterData] = v
    for {
      // remove no life characters and with max limit
      removedNoLifeAndLimit <- Future.successful {
        characters.filterNot(x => x._2.isNew == true || x._2.life <= 0 || x._2.count >= x._2.limit)
      }
      // make sure no eliminated or withdrawn characters on the list
      removeEliminatedOrWithdrawn <- Future.successful {
        removedNoLifeAndLimit.filterNot(x => x._2.status == 2  || x._2.status == 3)
      }
    } yield (removeEliminatedOrWithdrawn)
  }

  private def battleProcess(characters: HashMap[String, GQCharacterData]): Future[Unit] = Future.successful {
    // val characters: HashMap[String, GQCharacterData] = params
    do {
      val player: (String, GQCharacterData) = characters.head
      if (characters.size == 1) {
        characters.remove(player._1)
        GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
      }
      else {
        val availableCharacters: HashMap[String, GQCharacterData] = characters
        val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
        val filteredDateForBattle: Instant = now.plusDays(-7).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        // remove his other owned characters from the list
        var removedOwned = availableCharacters.filter(_._2.owner != player._2.owner)
        // check chracters spicific history to avoid battling again as posible..
        Await.ready(gQGameHistoryRepo.getByUsernameCharacterIDAndDate(player._2.owner,
                                                                      player._1,
                                                                      filteredDateForBattle.getEpochSecond,
                                                                      now.toInstant(ZoneOffset.UTC).getEpochSecond), Duration.Inf)
            .andThen {
              case Success(v) =>
                removedOwned = removedOwned
                                  .filterNot(ch => v.map(_.loserID).contains(ch._1))
                                  .filterNot(ch => v.map(_.winnerID).contains(ch._1))

              case Failure(e) => ()
            }
        // delay to make sure process is finished
        Thread.sleep(1000)

        if (!removedOwned.isEmpty) {
          val enemy: (String, GQCharacterData) = removedOwned.head
          val battle: GQBattleCalculation[GQCharacterData] = new GQBattleCalculation[GQCharacterData](player._2, enemy._2)

          if (!battle.result.equals(None)) {
            battle.result.map(x => println(x.characters.size))
            // save result into battleCounter
            battle.result.map(x => GQBattleScheduler.battleCounter.addOne(x.id, x))
          }
          else {
            GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
            GQBattleScheduler.noEnemy.addOne(enemy._1, enemy._2.owner)
          }
          characters.remove(player._1)
          characters.remove(enemy._1)
        }
        else {
          characters.remove(player._1)
          GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
        }
      }
    } while (!characters.isEmpty);
  }

  // recursive request no matter how many times till finished
  private def getEOSTableRows(sender: Option[String]): Future[Unit] = Future.successful {
    var hasNextKey: Option[String] = None
    var hasRows: Seq[GQTable] = Seq.empty
    do {
      Await.ready(requestTableRow(new TableRowsRequest(Config.GQ_CODE,
                                                      Config.GQ_TABLE,
                                                      Config.GQ_SCOPE,
                                                      None,
                                                      Some("uint64_t"),
                                                      None,
                                                      None,
                                                      hasNextKey), sender), Duration.Inf) map {
        case Some(GQRowsResponse(rows, hasNext, nextKey, sender)) =>
          hasNextKey = if (nextKey == "") None else Some(nextKey)
          hasRows = rows
        case _ =>
          hasNextKey = None
          hasRows = Seq.empty
      }

      Thread.sleep(2000)
      if (!hasRows.isEmpty) Await.ready(processEOSTableResponse(sender, hasRows), Duration.Inf)
    } while (hasNextKey != None);
  }

  private def processEOSTableResponse(sender: Option[String], rows: Seq[GQTable]): Future[Seq[Any]] = Future.sequence {
    rows.map { row =>
      // find account info and return ID..
      accountRepo.getByName(row.username).map {
        case Some(account) =>
          // val username: String = row.username
          val data: GQGame = row.data

          val characters = data.characters.map { ch =>
            val key = ch.key
            val time = ch.value.createdAt
            new GQCharacterData(key,
                                account.id,
                                ch.value.life,
                                ch.value.hp,
                                ch.value.`class`,
                                ch.value.level,
                                ch.value.status,
                                ch.value.attack,
                                ch.value.defense,
                                ch.value.speed,
                                ch.value.luck,
                                ch.value.limit,
                                ch.value.count,
                                if (time <= Instant.now().getEpochSecond - (60 * 5)) false else true,
                                time)
          }

          sender match {
            case Some("REQUEST_ON_BATTLE") =>
              characters.map(v => GQBattleScheduler.characters.addOne(v.key, v))
            case Some("REQUEST_REMOVE_NO_LIFE") =>
              characters.map(v => GQBattleScheduler.eliminatedOrWithdrawn.addOne(v.key, v))
            case Some("REQUEST_UPDATE_CHARACTERS_DB") =>
              characters.map(v => GQBattleScheduler.isUpdatedCharacters.addOne(v.key, v))
            case e => log.info("GQRowsResponse: unknown data")
          }

        case _ => Seq.empty
      }
    }
  }

  private def requestTableRow(req: TableRowsRequest, sender: Option[String]): Future[Option[GQRowsResponse]] =
    eosioHTTPSupport.getTableRows(req, sender)
  private def systemBattleScheduler(timer: FiniteDuration): Unit = {
    system.scheduler.scheduleOnce(timer) {
      println("GQ BattleScheduler Starting")
      GQBattleScheduler.nextBattle = 0
      self ! REQUEST_BATTLE_NOW
    }
  }
  private def defaultSchedule(): Unit = {
    GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTimeSet)
    systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
  }
}