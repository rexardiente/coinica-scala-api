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
  def props(out: Option[ActorRef])(implicit system: ActorSystem) =
    Props(classOf[DynamicBroadcastActor], out, system)
}

@Singleton
class DynamicBroadcastActor@Inject()(out: Option[ActorRef])(implicit system: ActorSystem) extends Actor {
  private val log: LoggingAdapter = Logging(context.system, this)

  override def preStart(): Unit = {
    super.preStart
    log.info(s"DynamicBroadcastActor Actor Initialized")
  }

  override def postStop(): Unit = {}

  import scala.reflect.ClassTag
  def receive: Receive = {
    // case all@(o: OverAllGameHistory)::rest =>
    //   all.asInstanceOf[List[OverAllGameHistory]].map(_.toJson)
    case all: OverAllGameHistory =>
      WebSocketActor.subscribers.foreach { case (id, actorRef) =>
        actorRef ! OutEvent(JsString("OVER_ALL_HISTORY_UPDATE"), all.toJson)
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

    case e => log.info("DynamicBroadcastActor: invalid request")
      // out.map(_ ! OutEvent(JsNull, JsString("invalid")))
  }
}