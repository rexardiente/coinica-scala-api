package akka

import javax.inject.{ Singleton, Inject }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.repo.UserAccountRepo
import akka.common.objects._
import utils.Config

object DynamicBroadcastActor {
  def props(userRepo: UserAccountRepo)(implicit system: ActorSystem) =
    Props(classOf[DynamicBroadcastActor], userRepo, system)
}

@Singleton
class DynamicBroadcastActor@Inject()(userRepo: UserAccountRepo)(implicit system: ActorSystem) extends Actor {
  private val log: LoggingAdapter = Logging(context.system, this)

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
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString(Config.GQ_GAME_CODE), Json.obj("STATUS" -> "BATTLE_FINISHED", "NEXT_BATTLE" -> GQBattleScheduler.nextBattle))
      }

    case "BROADCAST_DB_UPDATED" =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString(Config.GQ_GAME_CODE), Json.obj("STATUS" -> "CHARACTERS_UPDATED"))
      }

    case ("BROADCAST_CHARACTER_NO_ENEMY", map: Map[_, _]) =>
      try {
        map.asInstanceOf[Map[UUID, HashMap[String, UUID]]].map {
          case (id: UUID, characters) =>
            userRepo.getByID(id).map {
              case Some(v) =>
                WebSocketActor.subscribers(v.name) !
                OutEvent(JsString(Config.GQ_GAME_CODE), Json.obj("CHARACTER_NO_ENEMY" -> JsArray(characters.map(x => JsString(x._1)).toSeq)))
              case None => ()
            }
        }
      } catch { case _: Throwable => {} }
    case "BROADCAST_NO_CHARACTERS_AVAILABLE" =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString(Config.GQ_GAME_CODE), Json.obj("STATUS" -> "NO_CHARACTERS_AVAILABLE", "NEXT_BATTLE" -> GQBattleScheduler.nextBattle))
      }

    case e => log.info("DynamicBroadcastActor: invalid request")
  }
}