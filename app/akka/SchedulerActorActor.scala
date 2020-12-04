package akka

import javax.inject.{ Inject, Singleton }
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.Logger
import utils.lib.EOSIOSupport
import models.domain.eosio.{ TableRowsRequest, TableRowsResponse, GQGame }

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(ws: WSClient)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  case class Connect(connection: ActorRef)
  case class Disconnect(disconnection: ActorRef)
  object GQBroadcast
  
  val logger       : Logger      = Logger(this.getClass())
  val eosioSupport :EOSIOSupport = new EOSIOSupport(ws)

  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => self ! GQBroadcast)
  }

  def receive: Receive = {
    case connect: Connect => 
      // Indicators if there's new Client Connected..
      // println(connect)

    case response: TableRowsResponse =>
      response.rows.map { user =>
        val username: String = user.username
        val gameData: GQGame = user.game_data

        println("TableRowsResponse")
      }

    case GQBroadcast =>
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
    case "GQ_user_tbl_update" => println("GQ_user_tbl_update")
    case _ => println("unkown data received!!!")
  }
}