package akka

import javax.inject.{ Singleton, Inject }
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.domain.eosio.GhostQuestCharacterValue
import models.repo.UserAccountRepo
import models.domain.wallet.support.ETHJsonRpc
import akka.common.objects._
import models.service.PlatformConfigService

object DynamicBroadcastActor {
  def props(userRepo: UserAccountRepo, platformConfig: PlatformConfigService)(implicit system: ActorSystem) =
    Props(classOf[DynamicBroadcastActor], userRepo, platformConfig, system)
}

@Singleton
class DynamicBroadcastActor@Inject()(userRepo: UserAccountRepo,
                                    platformConfig: PlatformConfigService,
                                    implicit val system: ActorSystem) extends Actor {
  private val log: LoggingAdapter = Logging(context.system, this)
  private val ghostQuestConfig: Future[Option[PlatformGame]] = platformConfig.getGameInfoByName("ghostquest")

  override def preStart(): Unit = {
    super.preStart
    log.info(s"DynamicBroadcastActor Actor Initialized")
  }

  def receive: Receive = {
    case history: OverAllGameHistory =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString("OVER_ALL_HISTORY_UPDATE"), history.toJson)
      }
    case history: Array[OverAllGameHistory] =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString("OVER_ALL_HISTORY_UPDATE"), Json.toJson(history.toSeq))
      }

    case "BROADCAST_NEXT_BATTLE" =>
      ghostQuestConfig.map { config =>
        WebSocketActor.subscribers.foreach { case (id, actorRef) =>
          actorRef ! OutEvent(JsString(config.map(_.code).getOrElse("unknown_game")), Json.obj("STATUS" -> "BATTLE_FINISHED", "NEXT_BATTLE" -> GhostQuestSchedulerActor.nextBattle))
        }
      }

    case "BROADCAST_DB_UPDATED" =>
      ghostQuestConfig.map { config =>
        WebSocketActor.subscribers.foreach { case (id, actorRef) =>
          actorRef ! OutEvent(JsString(config.map(_.code).getOrElse("unknown_game")), Json.obj("STATUS" -> "CHARACTERS_UPDATED"))
        }
      }

    case ("BROADCAST_CHARACTER_NO_ENEMY", map: Map[_, _]) =>
      try {
        map.asInstanceOf[Map[GhostQuestCharacterValue, HashMap[String, GhostQuestCharacterValue]]].map {
          case (v: GhostQuestCharacterValue, characters) =>
            userRepo.getByGameID(v.owner_id).map {
              case Some(v) =>
                ghostQuestConfig.map { config =>
                  WebSocketActor.subscribers(v.id) !
                  OutEvent(JsString(config.map(_.code).getOrElse("unknown_game")), Json.obj("CHARACTER_NO_ENEMY" -> JsArray(characters.map(_._2.toJson).toSeq)))
                }
              case None => ()
            }
        }
      } catch { case _: Throwable => {} }
    case "BROADCAST_NO_CHARACTERS_AVAILABLE" =>
      ghostQuestConfig.map { config =>
        WebSocketActor.subscribers.foreach { case (id, actorRef) =>
          actorRef ! OutEvent(JsString(config.map(_.code).getOrElse("unknown_game")), Json.obj("STATUS" -> "NO_CHARACTERS_AVAILABLE", "NEXT_BATTLE" -> GhostQuestSchedulerActor.nextBattle))
        }
      }
    case ("BROADCAST_EMAIL_UPDATED", id: UUID, email: String) =>
      WebSocketActor
        .subscribers
        .filter(_._1 == id)
        .headOption
        .map { case (id, actorRef) => actorRef ! OutEvent(JsString("BROADCAST_EMAIL_UPDATED"), JsString(email)) }

    case e => log.info("DynamicBroadcastActor: invalid request")
  }
}