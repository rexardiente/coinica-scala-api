package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import com.typesafe.config.{ Config, ConfigFactory }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import play.api.libs.ws.WSClient
import models.domain.OutEvent
import models.domain.eosio.{ TableRowsRequest, GQCharacterGameHistory, GQBattleResult }
import models.domain.{ OverAllGameHistory, GameType, GQGameHistory }
import models.repo.OverAllGameHistoryRepo
import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
import models.service.GQSmartContractAPI
import utils.lib.{ EOSIOSupport, GQBattleCalculation }
import models.domain.eosio.GQ.v2._
import akka.common.objects._

object GQSchedulerActorV2 {
  var isIntialized: Boolean = false
  val defaultTimer: FiniteDuration = {ConfigFactory.load().getInt("platform.games.GQ.battle.timer")}.minutes
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
            gameTxHistory: OverAllGameHistoryRepo,
            eosioHTTPSupport: EOSIOHTTPSupport)(implicit system: ActorSystem) =
    Props(classOf[GQSchedulerActorV2], characterRepo, historyRepo, gameTxHistory, eosioHTTPSupport, system)
}

@Singleton
class GQSchedulerActorV2 @Inject()(
      characterRepo: GQCharacterDataRepo,
      gQGameHistoryRepo: GQCharacterGameHistoryRepo,
      gameTxHistory: OverAllGameHistoryRepo,
      eosioHTTPSupport: EOSIOHTTPSupport)(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val dynamicBroadcastActor: ActorRef = system.actorOf(Props(classOf[DynamicBroadcastActor], None, system))
  val eosTblRowsRequest: TableRowsRequest = new TableRowsRequest(
                                                  "ghostquest",
                                                  "users",
                                                  "ghostquest",
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
          // scheduled 5minutes to start battle..
          systemBattleScheduler(GQSchedulerActorV2.defaultTimer)
          // set true if actor already initialized
          // GQSchedulerActor.isIntialized = true
          log.info("GQSchedulerActorV2 Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    case REQUEST_BATTLE_NOW => GQBattleScheduler.REQUEST_BATTLE_STATUS match {
      case "ON_UPDATE" =>
      {
        self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_ON_BATTLE"))
      }

      case "REQUEST_ON_BATTLE" =>
      {
        val characters: HashMap[String, GQCharacterData] = GQBattleScheduler.characters
        val filtered = characters.filterNot(x => x._2.isNew == true || x._2.life <= 0 || x._2.count >= x._2.limit)
        // make sure no eliminated or withdrawn characters on the list
        val isEliminatedOrWithdrawn = filtered.filterNot(x => x._2.status == 2  || x._2.status == 3)
        // shuffle all list of characters to play
        val availableCharacters = scala.util.Random.shuffle(isEliminatedOrWithdrawn)
        // start the battle..
        battleProcess(availableCharacters)
        // all finished battle will be recorded into `battleCounter`
        // save all characters that has no available to play
        GQBattleScheduler.characters.clear
        GQBattleScheduler.REQUEST_BATTLE_STATUS = "REQUEST_BATTLE_DONE"
        self ! REQUEST_BATTLE_NOW
      }

      case "REQUEST_BATTLE_DONE" =>
      {
        val toProcessCounter = GQBattleScheduler.battleCounter

        toProcessCounter.foreach { count =>
          val winner = count._2.characters.filter(_._2._2).head
          val loser = count._2.characters.filter(!_._2._2).head

          val battle = eosioHTTPSupport.battleResult(count._1.toString, (winner._1, winner._2._1), (loser._1, loser._2._1))
          Thread.sleep(1000)

          battle.map {
            case Some(e) => ()
            case e => GQBattleScheduler.battleCounter.remove(count._1)
          }
        }
        // remove failed txs on the list before inserting to DB OverAllGameHistory
        saveHistoryDB(GQBattleScheduler.battleCounter)
      }
      case e => log.info(e)
    }

    case REQUEST_TABLE_ROWS(req, sender) =>
    {
      eosioHTTPSupport
            .getTableRows(req, sender)
            .map(_.map(self ! _).getOrElse({
              // broadcast to all connected users the next GQ battle
              dynamicBroadcastActor ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
              if (GQBattleScheduler.REQUEST_BATTLE_STATUS == "") {
                GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * 5)
                systemBattleScheduler(GQSchedulerActorV2.defaultTimer)
              }
            }))
    }

    case GQRowsResponse(rows, hasNext, nextKey, sender) =>
    {
      rows.foreach { row =>
        val username: String = row.username
        val data: GQGame = row.data

        val characters = data.characters.map { ch =>
          val key = ch.key
          val time = ch.value.createdAt
          new GQCharacterData(key,
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
        }
        sender match {
          case Some("REQUEST_ON_BATTLE") =>
            characters.map(v => GQBattleScheduler.characters.addOne(v.key, v))
          // case Some("REQUEST_BATTLE_NOW") =>
          //   characters.map(v => GQBattleScheduler.characters.addOne(v.key, v))
          case Some("REQUEST_REMOVE_NO_LIFE") =>
            characters.map(v => GQBattleScheduler.eliminatedOrWithdrawn.addOne(v.key, v))
          case Some("REQUEST_UPDATE_CHARACTERS_DB") =>
            characters.map(v => GQBattleScheduler.isUpdatedCharacters.addOne(v.key, v))
          case _ => log.info("GQRowsResponse: unknown data")
        }
      }

      if (hasNext) self ! REQUEST_TABLE_ROWS(new TableRowsRequest("ghostquest",
                                                                  "users",
                                                                  "ghostquest",
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
          // save to another variable and clear the dynamic result
          case Some("REQUEST_UPDATE_CHARACTERS_DB") =>
            val toSeq: Seq[GQCharacterData] = GQBattleScheduler.isUpdatedCharacters.map(_._2).toSeq
            characterRepo.updateOrInsertAsSeq(toSeq)
            GQBattleScheduler.isUpdatedCharacters.clear

          case _ =>
        }
      }
    }

    case REQUEST_CHARACTER_ELIMINATE =>
    {
      val filteredByStatus = GQBattleScheduler.eliminatedOrWithdrawn.filter(x => x._2.life <= 0)
      val successToRemove: ListBuffer[GQCharacterData] = new ListBuffer()
      // remove from smartcontract
      filteredByStatus.map { case (id, data) =>
        eosioHTTPSupport.eliminate(data.owner, id).map {
          case Some(txID) =>
            // remove from Charater DB and move to history
            characterRepo.remove(data.owner, id)
            characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
          case None => log.info("Error: removing character from smartcontract")
        }
      }
      // println(successToRemove.size)
      // // insert to History if succesfully removed
      // successToRemove.map(v => {
      //   println("successToRemove" + v.key)
      //   characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(v))
      // })
      // clean back the list to get ready for later battle schedule..
      GQBattleScheduler.eliminatedOrWithdrawn.clear
      // update character DB if it has new characters added..
      self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_UPDATE_CHARACTERS_DB"))
      // on battle start reset timer to 0 and set new timer until the battle finished
      // set back the time for next battle..
      GQBattleScheduler.REQUEST_BATTLE_STATUS = ""
      GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * 5)
      // broadcast to all connected users the next GQ battle
      dynamicBroadcastActor ! "BROADCAST_NEXT_BATTLE"
      systemBattleScheduler(GQSchedulerActorV2.defaultTimer)
    }
    case _ => ()
  }

  def saveHistoryDB(data: HashMap[UUID, GQBattleResult]): Unit = {
    data.map { count =>
      val winner = count._2.characters.filter(_._2._2).head
      val loser = count._2.characters.filter(!_._2._2).head
      val time = Instant.now
      ((new OverAllGameHistory(
                            UUID.randomUUID,
                            count._1,
                            "ghostquest",
                            GQGameHistory(winner._1, "WIN", true),
                            true,
                            time),
        new OverAllGameHistory(
                            UUID.randomUUID,
                            count._1,
                            "ghostquest",
                            GQGameHistory(loser._1, "WIN", false),
                            true,
                            time)),
        new GQCharacterGameHistory(
                      count._1.toString, // Game ID
                      winner._2._1,
                      winner._1,
                      loser._2._1,
                      loser._1,
                      count._2.logs,
                      time.getEpochSecond))
    }.map { case ((winner, loser), character) =>
      // insert Tx and character contineously
        // broadcast game result to connected users
        // use live data to feed on history update..
      for {
        _ <- gQGameHistoryRepo.insert(character)
        _ <- gameTxHistory
              .add(winner)
              .map(x => if (x > 0) dynamicBroadcastActor ! winner else log.info("Error: Game Tx Insertion"))

        _ <- gameTxHistory
                .add(loser)
                .map(x => if (x > 0) dynamicBroadcastActor ! loser else log.info("Error: Game Tx Insertion"))
      } yield ()
      Thread.sleep(500)
    }

    // broadcast to spicific user if his characters doesnt have enemy..
    dynamicBroadcastActor ! ("BROADCAST_CHARACTER_NO_ENEMY", GQBattleScheduler.noEnemy.groupBy(_._2))

    GQBattleScheduler.battleCounter.clear
    GQBattleScheduler.noEnemy.clear
    self ! REQUEST_TABLE_ROWS(eosTblRowsRequest, Some("REQUEST_REMOVE_NO_LIFE"))
  }

  def battleProcess(params: HashMap[String, GQCharacterData]): Unit = {
    // val characters: HashMap[String, GQCharacterData] = params
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
        gQGameHistoryRepo.getByUsernameAndCharacterID(player._1, player._2.owner).map { history =>
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
  }

  def systemBattleScheduler(timer: FiniteDuration): Unit = {
    if (GQBattleScheduler.REQUEST_BATTLE_STATUS == "")
    {
      system.scheduler.scheduleOnce(timer) {
        GQBattleScheduler.REQUEST_BATTLE_STATUS = "ON_UPDATE"
        GQBattleScheduler.nextBattle = 0
        self ! REQUEST_BATTLE_NOW
      }
    }
  }
}