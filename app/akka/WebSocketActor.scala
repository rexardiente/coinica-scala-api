package akka

import javax.inject.{ Singleton, Inject }
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.domain.enum._
import models.domain.Event._
import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
import models.repo.OverAllGameHistoryRepo
import models.service.UserAccountService
import models.service.GQSmartContractAPI
import akka.common.objects.{ Connect, GQBattleScheduler }
import utils.lib.EOSIOSupport

object WebSocketActor {
  def props(
      out: ActorRef,
      userAccountService: UserAccountService,
      characterRepo: GQCharacterDataRepo,
      historyRepo: GQCharacterGameHistoryRepo,
      overAllGameHistory: OverAllGameHistoryRepo,
      eosioHTTPSupport: EOSIOHTTPSupport)(implicit system: ActorSystem) =
    Props(classOf[WebSocketActor],
          out,
          userAccountService,
          characterRepo,
          historyRepo,
          overAllGameHistory,
          eosioHTTPSupport,
          system)

  val subscribers = scala.collection.mutable.HashMap.empty[String, ActorRef]
}

@Singleton
class WebSocketActor@Inject()(
      out: ActorRef,
      userAccountService: UserAccountService,
      characterRepo: GQCharacterDataRepo,
      historyRepo: GQCharacterGameHistoryRepo,
      overAllGameHistory: OverAllGameHistoryRepo,
      eosioHTTPSupport: EOSIOHTTPSupport)(implicit system: ActorSystem) extends Actor {
  private val code: Int = out.hashCode
  private val log: LoggingAdapter = Logging(context.system, this)
  private val characterUpdateActor: ActorRef = system.actorOf(
                                        Props(classOf[GQSchedulerActorV2],
                                              characterRepo,
                                              historyRepo,
                                              overAllGameHistory,
                                              eosioHTTPSupport,
                                              system))

  override def preStart(): Unit = {
    super.preStart
    // Insert into the db active users..
    // check if succesfully inserted
    // else send message and close connection
  	// generate new session and send to user
  	// use it for the next ws request...
    log.info(s"${code} ~> WebSocket Actor Initialized")
  	out ! OutEvent(JsNull, JsString("connected"))
  }

  override def postStop(): Unit = {
    log.error(s"${code} ~> Connection closed")
    // Remove user from subscribers
    WebSocketActor
      .subscribers
      .filter(x => x._2 == out)
      .map(user => WebSocketActor.subscribers.remove(user._1))
  }

  def receive: Receive = {
    case ev: Event =>
      try {
        ev match {
          case in: InEvent =>
            val user: String = in.id.as[String]
            // check if id/user is subscribed else do not allow
            WebSocketActor.subscribers.exists(x => x._1 == user) match {
              case false => out ! OutEvent(JsString(user), JsString("unauthorized"))
              case true =>
                in.input.as[InEventMessage] match {
                  // if server got a WS message for newly created character
                  // try to update character DB
                  case cc: GQCharacterCreated =>
                    characterUpdateActor ! akka.common.objects.REQUEST_TABLE_ROWS(GQSchedulerActorV2.eosTblRowsRequest, Some("REQUEST_UPDATE_CHARACTERS_DB"))
                    Thread.sleep(2000)
                    out ! OutEvent(JsNull, JsString("characters updated"))

                  // if result is empty it means on battle else standby mode..
                  case cc: GQGetNextBattle =>
                    if (GQBattleScheduler.nextBattle == 0)
                      out ! OutEvent(JsString("GQ"), Json.obj("STATUS" -> "ON_BATTLE", "NEXT_BATTLE" -> 0))
                    else
                      out ! OutEvent(JsString("GQ"), Json.obj("STATUS" -> "BATTLE_STANDY", "NEXT_BATTLE" -> GQBattleScheduler.nextBattle))

                  case e: EOSNotifyTransaction =>
                    // Check if notification relates to TH
                    // save into DB if found..
                    println("EOSNotifyTransaction" + e)

                  // send out the message to self and process separately..
                  case vip: VIPWSRequest => self ! vip
                  case _ => self ! "invalid"
                }
            }

          case oe: OutEvent =>
            log.info(oe.toJson.toString)

          case ca: ConnectionAlive =>
            log.info(s"${code} ~> Connection Reset")
            out ! OutEvent(JsNull, JsString("connection reset"))

          case Subscribe(id, msg) =>
            log.info(s"${id} ~> Subscribe")
            if (id == "default")
              out ! OutEvent(JsString(id), JsNull)
            else
              // add subscriber to subscribers list
              WebSocketActor.subscribers.exists(x => x._1 == id) match {
                case true =>
                  // update its akka actorRef if already exists
                  WebSocketActor.subscribers(id) = out
                  out ! OutEvent(JsString(id), JsString("already subscribed"))
                case _ =>
                  WebSocketActor.subscribers.addOne(id -> out)
                  out ! OutEvent(JsString(id), JsString(msg))
                  // check if user connects doenst exists else do nothing..
                  self ! Connect(id)
              }

          case _ =>
            log.info("Unknown")
        }
      } catch {
        case e: Throwable => out ! OutEvent(JsNull, JsString("invalid"))
      }

    case VIPWSRequest(id, cmd, req) => req match {
      case "info" =>
      case "current_rank" =>
      case "next_rank" =>
      case "payout" =>
      case "point" =>
    }
    // save new users into DB users and create VIP profile
    case Connect(user) =>
      userAccountService.isExist(user).map(x => if(!x) {
        val acc: UserAccount = UserAccount(user)
        userAccountService
          .newUserAcc(acc)
          .map(_ => userAccountService.newVIPAcc(VIPUser(acc.id, VIP.Bronze, VIP.Bronze, 0, 0, Instant.now)))
      })

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