package akka

import javax.inject.{ Singleton, Inject }
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain.{ Event, OutEvent, ConnectionAlive, InEvent, GQCharacterCreated, InEventMessage }
import models.domain.Event._
import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
import models.service.GQSmartContractAPI
import utils.lib.EOSIOSupport

object WebSocketActor {
  def props(
      out: ActorRef,
      characterRepo: GQCharacterDataRepo,
      historyRepo: GQCharacterGameHistoryRepo,
      eosio: EOSIOSupport,
      smartcontract: GQSmartContractAPI)(implicit system: ActorSystem) =
    Props(classOf[WebSocketActor], out, characterRepo, historyRepo, eosio, smartcontract, system)
}

@Singleton
class WebSocketActor@Inject()(
      out: ActorRef,
      characterRepo: GQCharacterDataRepo,
      historyRepo: GQCharacterGameHistoryRepo,
      eosio: EOSIOSupport,
      gqSmartContractAPI: GQSmartContractAPI)(implicit system: ActorSystem) extends Actor {
  private val code: Int = out.hashCode
  private val log: LoggingAdapter = Logging(context.system, this)
  private val characterUpdateActor: ActorRef = system.actorOf(
                                        Props(classOf[SchedulerActor],
                                              characterRepo,
                                              historyRepo,
                                              eosio,
                                              gqSmartContractAPI,
                                              system))

  override def preStart(): Unit = {
    super.preStart
    // Insert into the db active users..
    // check if succesfully inserted
    // else send message and close connection
  	// generate new session and send to user
  	// use it for the next ws request...
  	out ! OutEvent(JsNull, JsString("subscribed"))
    log.info(s"${code} ~> WebSocket Actor Initialized")
  }

  override def postStop(): Unit = {
    // Remove user from db active users..
    log.error(s"${code} ~> Connection closed")
  }

  def receive: Receive = {
    case ev: Event =>
      // TODO: check if user is subscribed else do not allow..
      try {
        ev match {
          case in: InEvent => in.input.as[InEventMessage] match {
              case cc: GQCharacterCreated =>
                // if server got a WS message for newly created character
                // try to update character DB
                characterUpdateActor ! akka.domain.common.objects.VerifyGQUserTable(SchedulerActor.eosTblRowsRequest)
                out ! OutEvent(JsString(code.toString), JsString("characters list updated"))
                // log.info("new character created")

              case _ =>
            }

          case oe: OutEvent =>
            log.info(oe.toJson.toString)

          case ca: ConnectionAlive =>
            log.info(s"${code} ~> Connection Reset")
            out ! OutEvent(JsString(code.toString), JsString("connection reset"))

          case _ =>
            log.info("Unknown")
        }
      } catch {
        case e: Throwable => out ! OutEvent(JsNull, JsString("invalid"))
      }

    case _ => out ! OutEvent(JsNull, JsString("invalid"))
  }
}

// Check if user session exist to use as reference for akka actor..
// save into DB for easy broadcast of messages..

// Broadcast into specific user if still active else broadcast all result.
// Bradcast all will be the best solution for game win and lose updates
// specified broadcast if theres only changes on specific user games

// January 13, 2021
// Save Connected users into DB and akka actorRef
// on subscribe check if user exist in DB(update if true) else insert new
// after GQ battle action, broadcast to all connected users the next battle schedule...
// how to handle if new user connected and track the exact time of the game..
// solution: create TBL for GQ_BATTLE_SCHEDULE(next_battle, total_active_user, total_que_characters)