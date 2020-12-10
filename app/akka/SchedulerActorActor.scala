package akka

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.Logger
import models.domain.eosio.{ TableRowsRequest, GQRowsResponse, GQGame, GQCharacterData }
import akka.domain.common.objects._
import utils.lib.EOSIOSupport

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(ws: WSClient)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  val logger       : Logger      = Logger(this.getClass())
  val eosioSupport :EOSIOSupport = new EOSIOSupport(ws)

  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // load GhostQuest users to DB update for one time.. in case server is down..
    val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    self ! LoadGQUserTable(req)

    // scheduled on every 3 minutes
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => self ! BattleScheduler)
  }

  def receive: Receive = {
    case connect: Connect => 
      // Indicators if there's new Client Connected..
      // println(connect)

    case GQRowsResponse(rows, hasNext, nextKey) =>
      var counter: Int = 0;
      for (counter <- counter until rows.size) {
        val username: String = rows(counter).username
        val gameData: GQGame = rows(counter).game_data

        val characters: Seq[GQCharacterData] = gameData.character.map { ch => 
          new GQCharacterData(
            java.util.UUID.randomUUID(), 
            ch.key,
            ch.value.owner,
            ch.value.character_life,
            ch.value.initial_hp,
            ch.value.hitpoints,
            ch.value.ghost_class,
            ch.value.ghost_level,
            ch.value.status,
            ch.value.attack,
            ch.value.defense,
            ch.value.speed,
            ch.value.luck,
            ch.value.prize,
            ch.value.battle_limit,
            ch.value.battle_count,
            ch.value.last_match,
            ch.value.enemy_fought)
        }

        println(characters.size)

        // define GQCharacterData object..
        // new GQCharacterData(java.util.UUID.randomUUI, )

      }

      // println(hasNext)
      // rows.map { user =>
      
      // }

      // Note: check if hasNext then LoadGQUserTable using nextKey

    case BattleScheduler =>
      // logger.info("Executing SchedulerActor in every 3 min...")
      // val req = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
      // println("BattleScheduler")
      // validate response and send to self
      // eosioSupport.getGQUsers(req).map(_.map(self ! _))

    // case UpdateUserDB(data) => 
    //   println(data)

    case LoadGQUserTable(request) =>
      eosioSupport.getGQUsers(request).map(_.map(self ! _))

    case "GQ_user_tbl_update" => println("GQ_user_tbl_update")
    case _ => println("unkown data received!!!")
  }
}