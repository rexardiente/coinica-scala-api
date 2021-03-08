package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ LocalTime, Instant }
import scala.util._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import models.domain.eosio._
import models.domain.{ GameTransactionHistory, GameType }
import models.repo.GameTransactionHistoryRepo
import models.repo.eosio._
import models.service.GQSmartContractAPI
import akka.common.objects._
import utils.lib.{ EOSIOSupport, GQBattleCalculation }
import models.domain.eosio.GQ.v2._

object SchedulerActor {
  var isIntialized: Boolean = false

  val eosTblRowsRequest: TableRowsRequest = new TableRowsRequest(
                                                  "ghostquest",
                                                  "users",
                                                  "ghostquest",
                                                  None,
                                                  Some("uint64_t"),
                                                  None,
                                                  None,
                                                  None)
  def props(characterRepo: GQCharacterDataRepo,
            historyRepo: GQCharacterGameHistoryRepo,
            gameTxHistory: GameTransactionHistoryRepo,
            eosio: EOSIOSupport,
            smartcontract: GQSmartContractAPI)(implicit system: ActorSystem) =
    Props(classOf[WebSocketActor], characterRepo, historyRepo, gameTxHistory, eosio, smartcontract, system)
}

@Singleton
class SchedulerActor @Inject()(
      characterRepo: GQCharacterDataRepo,
      gQGameHistoryRepo: GQCharacterGameHistoryRepo,
      gameTxHistory: GameTransactionHistoryRepo,
      support: EOSIOSupport,
      eosio: GQSmartContractAPI)(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

  override def preStart: Unit = {
    super.preStart
    // check if intializer is the SchedulerActor module..
    system.actorSelection("/user/SchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!SchedulerActor.isIntialized) {
          // scheduled 5minutes to start battle..
          system.scheduler.scheduleOnce(5.minutes) {
            GQBattleScheduler.battleStatus = "on_update"
            self ! SchedulerStatus("BattleScheduler")
          }
          // 24hrs Scheduler at 6:00AM in the morning daily..
          val dailySchedInterval: FiniteDuration = 24.hours
          val dailySchedDelay   : FiniteDuration = {
              val time = LocalTime.of(17, 0).toSecondOfDay
              val now = LocalTime.now().toSecondOfDay
              val fullDay = 60 * 60 * 24
              val difference = time - now
              if (difference < 0) {
                fullDay + difference
              } else {
                time - now
              }
            }.seconds
          system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => self ! DailyScheduler)
          // set true if actor already initialized
          SchedulerActor.isIntialized = true
          log.info("Ghost Quest Scheduler Actor Initialized")
        }

      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    case SchedulerStatus(request) => request match {
        case "BattleScheduler" => GQBattleScheduler.battleStatus match {
            case "on_update" =>
              GQBattleScheduler.nextBattle = 0 // reset timer to zero
              self ! OnUpdateGQList("onupdate")

            case "to_battle" =>
              self ! OnUpdateGQList("onbattle")

            case "GQ_battle_finished" => {
              support.unlockWalletAPI()
              // insert batch mode
              // Insert first on smartcontract, track where the insertion failed
              // failed tx will be removed to list and will not be added to DB
              GQBattleScheduler.battleCounter.map { count =>
                val winner = count._2.characters.filter(_._2._2).head
                val loser = count._2.characters.filter(!_._2._2).head
                val request: Seq[(String, String)] = Seq((winner._1, winner._2._1), (loser._1, loser._2._1))

                eosio.battleAction(count._1, request).map {
                  case Some(e) =>
                  case e =>
                    GQBattleScheduler.smartcontractTxFailed += count._1
                    // track failed tx here..
                    log.error("Error: Battle ")
                }
                Thread.sleep(300)
              }
              // remove failed txs on the list before inserting to DB
              GQBattleScheduler.battleCounter.filterNot(ch => GQBattleScheduler.smartcontractTxFailed.contains(ch._1))
              Thread.sleep(2000)
              // convert into GameTransactionHistory
              saveHistoryDB(GQBattleScheduler.battleCounter)
            }

            case "GQ_insert_DB" =>
              Thread.sleep(1000)
              support.lockAllWallets()

              // scheduled on start..
              // on battle start reset timer to 0 and set new timer until the battle finished
              // set back the time for next battle..
              GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * 5)
              system.scheduler.scheduleOnce(5.minutes) {
                GQBattleScheduler.battleStatus = "on_update"
                self ! SchedulerStatus("BattleScheduler")
              }
              // scheduled another battle

            case _ => // "GQ_insert_DB"
              // do nothing
          }

        case _ =>
      // send notify if charcters are being updated to avoid conflict
      // eosio.getGQUsers()
      // on first load, check if DB is updated
      // fetch and validate latest chracters as isNew = true on smartcontract
    }


    case OnUpdateGQList(req) => req match {
      case "onupdate" =>
        println("Starting onupdate")
        self ! VerifyGQUserTable(SchedulerActor.eosTblRowsRequest, Some(req))

      case "RemoveCharacterWithNoLife" =>
        self ! VerifyGQUserTable(SchedulerActor.eosTblRowsRequest, Some(req))

      case "onbattle" =>
        val characters: HashMap[String, GQCharacterData] = GQBattleScheduler.characters
        // remove characters that are newly created..
        val removedNew = characters.filterNot(_._2.isNew)
        // remove characters that has no life
        val removedNoLife = characters.filterNot(_._2.life <= 0)
        // remove characters that exceeds battle limit
        val removedExceedBattleLimit = characters.filter(x => x._2.count < x._2.limit)
        // make sure no eliminated or withdrawn characters on the list
        val removedPlayed: HashMap[String, GQCharacterData] =
          characters.filterNot(x => x._2.status == 2  || x._2.status == 3)
        // shuffle all list of characters to play
        val availableCharacters = scala.util.Random.shuffle(removedPlayed)
        // start the battle..
        battleProcess(availableCharacters)
        Thread.sleep(2000)
        // all finished battle will be recorded into `battleCounter`
        // save all characters that has no available to play
        GQBattleScheduler.characters.clear
        GQBattleScheduler.battleStatus = "GQ_battle_finished"
        self ! SchedulerStatus("BattleScheduler")

      case "Unknown" =>
        println("OnUpdateGQList Unknown")
    }

    case GQRowsResponse(rows, hasNext, nextKey, sender) => {
      rows.foreach { row =>
        val username: String = row.username
        val data: GQGame = row.data

        data.characters.foreach { ch =>
          val key = ch.key
          val time = ch.value.createdAt
          val chracterInfo = new GQCharacterData(
                                key,
                                username,
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
          sender match {
            case Some("onupdate") => GQBattleScheduler.characters.addOne(key, chracterInfo)
            case Some("RemoveCharacterWithNoLife") => GQBattleScheduler.eliminatedOrWithdrawn.addOne(key, chracterInfo)
            case Some("update_characters") =>
              // TODO: chracters Updated on user request...
              println("chracters Updated: "+ ch.key)
            case _ => println("Unknown Data")
          }
        }
      }

       if (hasNext) self ! VerifyGQUserTable(new TableRowsRequest("ghostquest",
                                                                  "users",
                                                                  "ghostquest",
                                                                  None,
                                                                  Some("uint64_t"),
                                                                  None,
                                                                  None,
                                                                  Some(nextKey)), sender)
       if (!hasNext) {
          sender match {
            case Some("onupdate") =>
              GQBattleScheduler.battleStatus = "to_battle"
              self ! SchedulerStatus("BattleScheduler")
              // self ! SchedulerStatus("BattleScheduler", Some("Smart contract characters validated."))
            case Some("RemoveCharacterWithNoLife") =>
              GQBattleScheduler.isRemoving = false
              self ! RemoveCharacterWithNoLife

            case Some("VerifyGQUserTable") =>
              println("VerifyGQUserTable: chracters Updated")
            case _ =>
              println("None hasNext")
          }
       }
    }

    case RemoveCharacterWithNoLife =>
      // set to remove status while fetching..
      if (GQBattleScheduler.isRemoving)
        self ! OnUpdateGQList("RemoveCharacterWithNoLife")
      else {
        // to make sure that validations is finished
        // Thread.sleep(5000)
        // filter characters with status eliminated or withdrawn
        val eliminatedOrWithdrawn: HashMap[String, GQCharacterData] = GQBattleScheduler.eliminatedOrWithdrawn
        val filteredByStatus = eliminatedOrWithdrawn.filter(x => x._2.life <= 0)
        // remove from smartcontract
        filteredByStatus.foreach { case (id, data) =>
          eosio.removeCharacter(data.owner, id).map {
            // Remove from the Character Data DB
            case Some(x) =>
              characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
            // TODO: check the reason why it is failed..
            case None => println("Error: removing character from smartcontract")
          }
        }
        // clean back the list to get ready for later battle schedule..
        GQBattleScheduler.eliminatedOrWithdrawn.clear
        GQBattleScheduler.isRemoving = false
        GQBattleScheduler.battleStatus = "GQ_insert_DB"
        self ! SchedulerStatus("BattleScheduler")
      }


    case VerifyGQUserTable(request, sender) =>
      eosio.getGQUsers(request, sender).map(_.map(self ! _))

    case DailyScheduler =>
      log.warning("Tracker every 5PM daily!!!!")

    case e =>
      println("Error: Unkown data received")
  }

  def battleProcess(params: HashMap[String, GQCharacterData]): Unit = {
    val characters: HashMap[String, GQCharacterData] = params
    do {
      val player: (String, GQCharacterData) = characters.head
      if (characters.size == 1) {
        characters.remove(player._1)
        GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
      }
      else {
        // remove his other owned characters from the list (remove 3 and 4 remaining)
        val removedOwned: HashMap[String, GQCharacterData] =
          characters.filterNot(_._2.owner == player._2.owner)

        gQGameHistoryRepo.getByUsernameAndCharacterID(player._1, player._2.owner).map { history =>
          removedOwned
            .filterNot(ch => history.map(_.loserID).contains(ch._1))
            .filterNot(ch => history.map(_.winnerID).contains(ch._1))
        }

        // add delay to avoid conflict when process takes too long..
        Thread.sleep(500)
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

  def saveHistoryDB(data: HashMap[UUID, GQBattleResult]): Unit = {
    data.map { count =>
      val winner = count._2.characters.filter(_._2._2).head
      val loser = count._2.characters.filter(!_._2._2).head
      val time = Instant.now
      (new GameTransactionHistory(
                      UUID.randomUUID,
                      count._1,
                      "ghostquest",
                      "url",
                      List(GameType(winner._1, true, 0.3), GameType(loser._1, false, 0.3)),
                      true,
                      time),
        new GQCharacterGameHistory(
                      count._1.toString, // Game ID
                      winner._2._1,
                      winner._1,
                      loser._2._1,
                      loser._1,
                      count._2.logs,
                      time.getEpochSecond))
    }.map { case (tx, char) =>
      // insert Tx and character contineously
      gQGameHistoryRepo.insert(char)
      gameTxHistory.add(tx)
    }

    Thread.sleep(5000)
    GQBattleScheduler.battleCounter.clear
    GQBattleScheduler.noEnemy.clear
    // remove eliminated characters on the smartcontract
    GQBattleScheduler.isRemoving = true
    self ! RemoveCharacterWithNoLife
  }

  def defaultThreadSleep(): Unit = Thread.sleep(1000)
}