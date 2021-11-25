package akka

import javax.inject.{ Singleton, Inject, Named }
import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.domain.enum._
import models.domain.Event._
import models.domain.eosio.TableRowsRequest
import models.repo.{ OverAllGameHistoryRepo, VIPUserRepo }
import models.service.{ UserAccountService, PlatformConfigService }
import models.domain.wallet.support.UserAccountWalletHistory
import utils.lib.GhostQuestEOSIO
// import utils.GameConfig

object WebSocketActor {
  def props(
      out: ActorRef,
      platformConfigService: PlatformConfigService,
      userAccountService: UserAccountService,
      overAllGameHistory: OverAllGameHistoryRepo,
      vipUserRepo: VIPUserRepo,
      ghostQuestEOSIO: GhostQuestEOSIO,
      dynamicBroadcast: ActorRef,
      dynamicProcessor: ActorRef)(implicit system: ActorSystem) =
    Props(classOf[WebSocketActor],
          out,
          platformConfigService,
          userAccountService,
          overAllGameHistory,
          vipUserRepo,
          ghostQuestEOSIO,
          dynamicBroadcast,
          dynamicProcessor,
          system)
  val subscribers = HashMap.empty[UUID, ActorRef]
  var hasPrevUpdateCharacter: Boolean = false
}

@Singleton
class WebSocketActor@Inject()(
      out: ActorRef,
      platformConfigService: PlatformConfigService,
      userAccountService: UserAccountService,
      overAllGameHistory: OverAllGameHistoryRepo,
      vipUserRepo: VIPUserRepo,
      ghostQuestEOSIO: GhostQuestEOSIO,
      dynamicBroadcast: ActorRef,
      dynamicProcessor: ActorRef)(implicit system: ActorSystem) extends Actor {
  private val code: Int = out.hashCode
  private val log: LoggingAdapter = Logging(context.system, this)
  // update config every initialization of thread.
  private var ghostquest: Option[PlatformGame] = None
  private var mahjonghilo: Option[PlatformGame] = None
  private var treasurehunt: Option[PlatformGame] = None

  override def preStart(): Unit = {
    super.preStart
    // load game configs..
    for {
      // load ghostquest game defaults..
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("ghostquest")
          .map(game => { ghostquest = game })
      }
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("mahjonghilo")
          .map(game => { mahjonghilo = game })
      }
      _ <- Future.successful {
        platformConfigService
          .getGameInfoByName("treasurehunt")
          .map(game => { treasurehunt = game })
      }
    } yield ()
    // Insert into the db active users..
    // check if succesfully inserted
    // else send message and close connection
  	// generate new session and send to user
  	// use it for the next ws request...
    log.info("WebSocket Actor Initialized")
  	out ! OutEvent(JsNull, JsString("connected"))
  }

  override def postStop(): Unit = {
    log.error("Connection closed")
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
          case withdraw: ETHUSDCWithdrawEvent =>
            SystemSchedulerActor.walletTransactions.addOne(withdraw.tx_hash, withdraw)
            out ! OutEvent.apply

          case deposit: DepositEvent =>
            SystemSchedulerActor.walletTransactions.addOne(deposit.tx_hash, deposit)
            out ! OutEvent.apply

          case in: InEvent =>
            val user: UUID = in.id.asOpt[UUID].getOrElse(UUID.randomUUID)
            // .as[String]
            // check if id/user is subscribed else do not allow
            WebSocketActor.subscribers.exists(x => x._1 == user) match {
              case false => out ! OutEvent(JsString(user.toString), JsString("unauthorized"))
              case true => in.input match {
                // if server got a WS message for newly created character
                // try to update character DB
                // case cc: GQCharacterCreated =>
                //   if (!WebSocketActor.hasPrevUpdateCharacter) {
                //     // set has existing request to update characters DB
                //     WebSocketActor.hasPrevUpdateCharacter = true
                //     Thread.sleep(300)
                //     Await.ready(getEOSTableRows(Some("REQUEST_UPDATE_CHARACTERS_DB")), Duration.Inf)
                //     Thread.sleep(1000)
                //     if (!WebSocketActor.isUpdatedCharacters.isEmpty) {
                //       val seq: Seq[GQCharacterData] = WebSocketActor.isUpdatedCharacters.map(_._2).toSeq
                //       // remove characters with no life..
                //       for {
                //         _ <- Future.sequence {
                //           seq.filter(x => x.life <= 0).map { data =>
                //             userAccountService.getAccountByID(data.owner).map {
                //               case Some(account) =>
                //                 Await.ready(for {
                //                   isRemoved <- characterRepo.remove(account.id, data.key)
                //                   _ <- Future.successful {
                //                     Thread.sleep(500)
                //                     // check if character exist already on history else add..
                //                     characterRepo.getCharacterHistoryByID(data.key).map {
                //                       case Some(v) => ()
                //                       case None =>
                //                         characterRepo
                //                           .insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
                //                           .map(x => if(x < 1) characterRepo.insert(data))
                //                     }
                //                   }
                //                 } yield (Thread.sleep(1000)), Duration.Inf)
                //               case _ => ()
                //             }
                //           }
                //         }
                //         // update remaining characters that are still on battle
                //         _ <- Future.sequence(seq.filter(x => x.life > 0).map(characterRepo.updateOrInsertAsSeq))
                //       } yield (out ! OutEvent.apply)
                //     }
                //     WebSocketActor.isUpdatedCharacters.clear
                //     WebSocketActor.hasPrevUpdateCharacter = false
                //   }
                // if result is empty it means on battle else standby mode..
                case cc: GQGetNextBattle =>
                  ghostquest.map { game =>
                    if (GhostQuestSchedulerActor.nextBattle == 0)
                      out ! OutEvent(JsString(game.code), Json.obj("STATUS" -> "ON_BATTLE", "NEXT_BATTLE" -> 0))
                    else
                      out ! OutEvent(JsString(game.code), Json.obj("STATUS" -> "BATTLE_STANDY", "NEXT_BATTLE" -> GhostQuestSchedulerActor.nextBattle))
                  }

                case e: EOSNotifyTransaction =>
                  // Check if notification relates to TH
                  // save into DB if found..
                  println("EOSNotifyTransaction" + e)
                // send out the message to self and process separately..
                case vip: VIPWSRequest => self ! vip
                case _ => ()
              }
            }

          case oe: OutEvent =>
            log.info(oe.toJson.toString)

          case ca: ConnectionAlive =>
            out ! OutEvent(JsNull, JsString("connection reset"))

          case Subscribe(id, msg) =>
            log.info(s"${id} ~> Subscribe")
            if (id == "default")
              out ! OutEvent(JsString(id.toString), JsNull)
            else
              // add subscriber to subscribers list
              WebSocketActor.subscribers.exists(x => x._1 == id) match {
                case true =>
                  // update its akka actorRef if already exists
                  WebSocketActor.subscribers(id) = out
                  out ! OutEvent(JsNull, Json.obj("error" -> "session exists"))
                case _ =>
                  WebSocketActor.subscribers.addOne(id -> out)
                  out ! OutEvent(JsString(id.toString), JsString(msg))
                  // check if user connects doenst exists else do nothing..
                  // self ! Connect(id)
              }

          case _ => log.info("Unknown")
        }
      } catch {
        case _: Throwable => out ! OutEvent(JsNull, JsString("invalid"))
      }

    case VIPWSRequest(id, cmd, req) => req match {
      case "info" =>
      case "current_rank" =>
      case "next_rank" =>
      case "payout" =>
      case "point" =>
    }

    case _ => out ! OutEvent(JsNull, JsString("invalid"))
  }
}