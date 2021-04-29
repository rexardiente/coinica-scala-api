package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneOffset, ZoneId }
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
  var isIntialized: Boolean = false
  val defaultTime: Int = Config.GQ_DEFAULT_BATTLE_TIMER
  val scheduledTime: FiniteDuration = { defaultTime }.minutes
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
  private val defaultTimeZone: ZoneId = ZoneOffset.UTC
  private val eosTblRowsRequest: TableRowsRequest = new TableRowsRequest(
                                                        Config.GQ_CODE,
                                                        Config.GQ_TABLE,
                                                        Config.GQ_SCOPE,
                                                        None,
                                                        Some("uint64_t"),
                                                        None,
                                                        None,
                                                        None)

  override def preStart: Unit = {
    super.preStart

    system.actorSelection("/user/GQSchedulerActorV2").resolveOne().onComplete {
      case Success(actor) =>
        if (!GQSchedulerActorV2.isIntialized) {
          // akka.stream.scaladsl.Source.tick(0.seconds, 20.seconds, "GQSchedulerActorV2").runForeach(n => removeNoLifeCharactersScheduler)
          // GQSchedulerActorV2.isIntialized = true
          // // scheduled to start battle based on settings..
          // self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_UPDATE_CHARACTERS_DB"))
          // Thread.sleep(5000)

          // GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
          // systemBattleScheduler(10.seconds)
          // // set true if actor already initialized
          log.info("GQ Scheduler Actor V2 Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    case REQUEST_BATTLE_NOW =>
      GQBattleScheduler.REQUEST_BATTLE_STATUS match {
        case "ON_UPDATE" => self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_ON_BATTLE"))

        case "REQUEST_ON_BATTLE" =>
        {
          val characters: HashMap[String, GQCharacterData] = GQBattleScheduler.characters
          val filtered = characters.filterNot(x => x._2.isNew == true || x._2.life <= 0 || x._2.count >= x._2.limit)
          // make sure no eliminated or withdrawn characters on the list
          val isEliminatedOrWithdrawn = filtered.filterNot(x => x._2.status == 2  || x._2.status == 3)
          // shuffle all list of characters to play
          if (!isEliminatedOrWithdrawn.isEmpty) {
            val randomCharacters = scala.util.Random.shuffle(isEliminatedOrWithdrawn)
            println("battleProcess start")
            Await.ready(battleProcess(randomCharacters), Duration.Inf)
            println("battleProcess successfully end")
            // start the battle..
            // if (!availableCharacters.isEmpty) battleProcess(availableCharacters)
            // all finished battle will be recorded into `battleCounter`
            // save all characters that has no available to play
            Thread.sleep(2000)
            GQBattleScheduler.REQUEST_BATTLE_STATUS = "REQUEST_BATTLE_DONE"
            GQBattleScheduler.characters.clear
            self ! REQUEST_BATTLE_NOW
          } else {
            dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
            GQBattleScheduler.characters.clear
            GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
            GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
            systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
          }
        }

        case "REQUEST_BATTLE_DONE" =>
        {
          if (GQBattleScheduler.battleCounter.isEmpty) {
            dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
            GQBattleScheduler.battleCounter.clear
            GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
            GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
            systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
          } else {
            val withTxHash: Future[Seq[(String, (UUID, GQBattleResult))]] =
            Future.sequence(GQBattleScheduler.battleCounter.map { count =>
              val winner = count._2.characters.filter(_._2._2).head
              val loser = count._2.characters.filter(!_._2._2).head

              for {
                p1 <- accountRepo.getByID(winner._2._1)
                p2 <- accountRepo.getByID(loser._2._1)
                process <- {
                  try {
                    eosioHTTPSupport.battleResult(count._1.toString, (winner._1, p1.get.name), (loser._1, p2.get.name)).map {
                      case Some(e) => (e, count)
                      case e => null
                    }
                  } catch {
                    case e: Throwable => Future(null)
                  }
                }
              } yield (process)
            }.toSeq).map(_.filterNot(_ == null))
            // remove failed txs on the list before inserting to DB OverAllGameHistory
            for {
              _ <- Future.successful(withTxHash.map(saveHistoryDB))
              // update system process
              _ <- Future.successful(withTxHash.map(insertOrUpdateSystemProcess))
              _ <- Future.successful(self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_REMOVE_NO_LIFE")))
            } yield (Thread.sleep(5000))
            // remove chracters that has no more available life..
            // self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_REMOVE_NO_LIFE"))
            // self ! REQUEST_CHARACTER_ELIMINATE
            // on battle start reset timer to 0 and set new timer until the battle finished
            // set back the time for next battle..
            GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
            GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
            // broadcast to all connected users the next GQ battle
            dynamicBroadcast ! "BROADCAST_NEXT_BATTLE"
            systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
          }
        }
        case e => log.info(e)
      }

    case REQUEST_TABLE_ROWS(req, sender) =>
    {
      eosioHTTPSupport
        .getTableRows(req, sender)
        .map(_.map(self ! _).getOrElse {
          // broadcast to all connected users the next GQ battle
          dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
          GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
          GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
          systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
        })
    }

    case GQRowsResponse(rows, hasNext, nextKey, sender) =>
    {
      rows.foreach { row =>
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

          case _ => ()
        }
      }

      Thread.sleep(3000)
      if (hasNext) self ! REQUEST_TABLE_ROWS(new TableRowsRequest(Config.GQ_CODE,
                                                                  Config.GQ_TABLE,
                                                                  Config.GQ_SCOPE,
                                                                  None,
                                                                  Some("uint64_t"),
                                                                  None,
                                                                  None,
                                                                  Some(nextKey)), sender)
      if (!hasNext) {
        sender match {
          case Some("REQUEST_ON_BATTLE") =>
            GQBattleScheduler.REQUEST_BATTLE_STATUS = "REQUEST_ON_BATTLE"
            self ! REQUEST_BATTLE_NOW
          case Some("REQUEST_REMOVE_NO_LIFE") =>
            self ! REQUEST_CHARACTER_ELIMINATE
          case Some("REQUEST_UPDATE_CHARACTERS_DB") =>
            val seq: Seq[GQCharacterData] = GQBattleScheduler.isUpdatedCharacters.map(_._2).toSeq
            seq.foreach(characterRepo.updateOrInsertAsSeq)
            GQBattleScheduler.isUpdatedCharacters.clear
          case _ =>
        }
      }
    }

    case REQUEST_CHARACTER_ELIMINATE =>
    {
      val filteredByStatus = GQBattleScheduler.eliminatedOrWithdrawn.filter(x => x._2.life <= 0)
      // remove from smartcontract
      filteredByStatus.foreach { case (id, data) =>
        for {
          account <- accountRepo.getByID(data.owner)
          _ <- {
            if (account != None) {
              val acc: UserAccount =  account.get

              eosioHTTPSupport.eliminate(acc.name, id).map {
                // track successfully remove in contract
                case Some(txHash) => GQBattleScheduler.toRemovedCharacters.addOne((id, data))
                  // characterRepo.remove(acc.id, id).map { isDeleted =>
                  //   println("characterRepo.remove >>> " + isDeleted)
                  //   if (isDeleted > 0) characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
                  //   else failedToRemove.addOne(data)
                  //     // characterRepo.insert(data)
                  // }
                case None => // log.info("Error: removing character from smartcontract")
              }
            }
            else Future(0)
          }
        } yield (Thread.sleep(1000))
      }
      // Thread.sleep(5000)
      // update character DB if it has new characters added..
      // self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_UPDATE_CHARACTERS_DB"))
      // val remainingCharacters = GQBattleScheduler.eliminatedOrWithdrawn.filter(x => x._2.life > 0).map(_._2).toSeq
      // characterRepo.updateOrInsertAsSeq(remainingCharacters)
      // clean back the list to get ready for later battle schedule..
      println(GQBattleScheduler.eliminatedOrWithdrawn.size)
      GQBattleScheduler.eliminatedOrWithdrawn.clear
      // on battle start reset timer to 0 and set new timer until the battle finished
      // set back the time for next battle..
      GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
      GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTime)
      // broadcast to all connected users the next GQ battle
      dynamicBroadcast ! "BROADCAST_NEXT_BATTLE"
      println("End Battle")
      systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
    }
    case _ => ()
  }

  def saveHistoryDB(data: Seq[(String, (UUID, GQBattleResult))]): Unit = {
    data.map { case (txHash, (gameID, result)) =>
      val winner = result.characters.filter(_._2._2).head
      val loser = result.characters.filter(!_._2._2).head
      val time = Instant.now.getEpochSecond

      (for {
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
                      time)))
      .map { case ((winner, loser), character) =>
        // insert Tx and character contineously
        // broadcast game result to connected users
        // use live data to feed on history update..
        for {
          _ <- gQGameHistoryRepo.insert(character)
          _ <- gameTxHistory.add(winner)
          _ <- gameTxHistory.add(loser)
        } yield (Thread.sleep(1500))
        // broadcast GQ game result..
        dynamicBroadcast ! Array(winner, loser)
      }
    }
    // broadcast to spicific user if his characters doesnt have enemy..
    dynamicBroadcast ! ("BROADCAST_CHARACTER_NO_ENEMY", GQBattleScheduler.noEnemy.groupBy(_._2))

    GQBattleScheduler.battleCounter.clear
    GQBattleScheduler.noEnemy.clear
  }
  // Insert into tasks daily tracker
  // tx hash, game id, battle result
  def insertOrUpdateSystemProcess(seq: Seq[(String, (UUID, GQBattleResult))]): Unit = {
    val createdAt: Instant = LocalDate.now().atStartOfDay().atZone(defaultTimeZone).toInstant
    seq.map { case (hash, (gameID, result)) =>
      result.characters.map(v => dynamicProcessor ! DailyTask(v._2._1, Config.GQ_GAME_ID, 1))
      // TODO: proper deduction per game wagered..
      // ChallengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double)
      // for {
      //   count <- gQGameHistoryRepo.getByUsernameAndCharacterID(v._2._1, v._1)
      // } yield ()
      result
        .characters
        .map(v => dynamicProcessor ! ChallengeTracker(v._2._1, 1, (if(v._2._2) 2 else 0), 1, (if(v._2._2) 0.5 else 0)))
    }
  }

  def battleProcess(params: HashMap[String, GQCharacterData]): Future[Unit] = Future {
    // val characters: HashMap[String, GQCharacterData] = params
    println(params.size)
    do {
      val player: (String, GQCharacterData) = params.head
      if (params.size == 1) {
        params.remove(player._1)
        GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
      }
      else {
        // remove his other owned characters from the list (remove 3 and 4 remaining)
        val removedOwned: HashMap[String, GQCharacterData] =
          params.filterNot(_._2.owner == player._2.owner)
        // check chracters spicific history to avoid battling again as posible..
        gQGameHistoryRepo.getByUsernameAndCharacterID(player._2.owner, player._1).map { history =>
          removedOwned
            .filterNot(ch => history.map(_.loserID).contains(ch._1))
            .filterNot(ch => history.map(_.winnerID).contains(ch._1))
        }
        // add delay to avoid conflict when process takes too long and repeating battles each params..
        .map(_ => Thread.sleep(5000))
        if (!removedOwned.isEmpty) {
          val enemy: (String, GQCharacterData) = removedOwned.head
          val battle: GQBattleCalculation[GQCharacterData] = new GQBattleCalculation[GQCharacterData](player._2, enemy._2)

          if (!battle.result.equals(None))
            // save result into battleCounter
            battle.result.map(x => GQBattleScheduler.battleCounter.addOne(x.id, x))
          else {
            GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
            GQBattleScheduler.noEnemy.addOne(enemy._1, enemy._2.owner)
          }
          params.remove(player._1)
          params.remove(enemy._1)
        }
        else {
          params.remove(player._1)
          GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
        }
      }
    } while (!params.isEmpty);
    println("battleProcess successfully finsihed")
  }

  def systemBattleScheduler(timer: FiniteDuration): Unit = {
    if (GQBattleScheduler.REQUEST_BATTLE_STATUS == "")
    {
      system.scheduler.scheduleOnce(timer) {
        println("Starting Battle")
        GQBattleScheduler.REQUEST_BATTLE_STATUS = "ON_UPDATE"
        GQBattleScheduler.nextBattle = 0
        self ! REQUEST_BATTLE_NOW
      }
    }
  }

  def removeNoLifeCharactersScheduler(): Unit = {
    // check if characters is not empty then process
    // if (GQBattleScheduler.toRemovedCharacters.size > 0)
    // check chracters DB has existing characters with no life
    // then add to the process
    for {
      characters <- Future(GQBattleScheduler.toRemovedCharacters.map(_._2).toSeq)
      hasNoLife <- characterRepo.getNoLifeCharacters
      _ <- Future.successful {
        val mergeSeq = characterRepo.mergeSeq[GQCharacterData, Seq[GQCharacterData]](characters, hasNoLife)

        if (!mergeSeq.isEmpty) {
          mergeSeq.foreach { data =>
            for {
              isRemoved <- characterRepo.remove(data.owner, data.key)
              result <- {
                if (isRemoved > 0) {
                  val newData: GQCharacterDataHistory = GQCharacterData.toCharacterDataHistory(data)
                  characterRepo.insertDataHistory(newData)
                }
                else Future(0) // TODO: re-try if failed tx.
              }
            } yield if (result > 0) Thread.sleep(1000)
          }

          GQBattleScheduler.toRemovedCharacters.clear
          // broadcast users that DB has been update..
          // update again overall DB to make sure its updated..
          self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_UPDATE_CHARACTERS_DB"))
          Thread.sleep(3000)
          dynamicBroadcast ! "BROADCAST_DB_UPDATED"
        }
        else println("No characters")
      }
    } yield ()
  }
}