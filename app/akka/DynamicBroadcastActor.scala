package akka

import javax.inject.{ Singleton, Inject }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import akka.common.objects._

object DynamicBroadcastActor {
  def props(implicit system: ActorSystem) =
    Props(classOf[DynamicBroadcastActor], system)
}

@Singleton
class DynamicBroadcastActor@Inject()(implicit system: ActorSystem) extends Actor {
  private val log: LoggingAdapter = Logging(context.system, this)

  override def preStart(): Unit = {
    super.preStart
    log.info(s"DynamicBroadcastActor Actor Initialized")
  }

  override def postStop(): Unit = {}

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
        actorRef ! OutEvent(JsString("GQ"), Json.obj("STATUS" -> "BATTLE_FINISHED", "NEXT_BATTLE" -> GQBattleScheduler.nextBattle))
      }

    case ("BROADCAST_CHARACTER_NO_ENEMY", map: Map[_, _]) =>
      try {
        map.asInstanceOf[Map[String, HashMap[String, String]]].map {
          case (user: String, characters) => WebSocketActor.subscribers(user) !
            OutEvent(JsString("GQ"), Json.obj("CHARACTER_NO_ENEMY" -> JsArray(characters.map(x => JsString(x._1)).toSeq)))
        }
      } catch { case _: Throwable => {} }
    case "BROADCAST_NO_CHARACTERS_AVAILABLE" =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString("GQ"), Json.obj("STATUS" -> "NO_CHARACTERS_AVAILABLE", "NEXT_BATTLE" -> GQBattleScheduler.nextBattle))
      }

    case e => log.info("DynamicBroadcastActor: invalid request")
      // out.map(_ ! OutEvent(JsNull, JsString("invalid")))
  }
}