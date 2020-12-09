package akka

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.Logger
import models.domain.eosio.{ TableRowsRequest, TableRowsResponse, GQGame }
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
    self ! LoadGQUserTable

    // scheduled on every 3 minutes
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 3.minute, interval = 3.minute)(() => self ! BattleScheduler)
  }

  def receive: Receive = {
    case connect: Connect => 
      // Indicators if there's new Client Connected..
      // println(connect)

    case response: TableRowsResponse =>
      response.rows.map { user =>
        val username: String = user.username
        val gameData: GQGame = user.game_data

        // println("TableRowsResponse")
      }

    case BattleScheduler =>
      // logger.info("Executing SchedulerActor in every 3 min...")
      val req = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
      // validate response and send to self
      eosioSupport.getGQUsers(req).map(_.asOpt[TableRowsResponse].map(self ! _))

    //   Clients.clientsList.append(connect.connection)
    //   self ! Json.obj("message"->"Connected")

    // case disconnect: Disconnect => // Indicators if there's a Client Disconnected..
    //   Clients.clientsList.find(_ == disconnect.disconnection).map(Clients.clientsList -= _)
    //   self ! Json.obj("message"->"Somebody is disconnected")

    // case json: JsValue => // Sends all the messages to all Clients..
    //   Clients.clientsList.map(_ ! json)
    case LoadGQUserTable =>
      println("LoadGQUserTable")

    case "GQ_user_tbl_update" => println("GQ_user_tbl_update")
    case _ => println("unkown data received!!!")
  }
}