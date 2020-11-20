package akka

import javax.inject.Singleton
import akka.actor._
import play.api.libs.json._
import models.domain.{ OutEvent, InEvent}

object WebSocketActor {
  def props[T <: ActorRef](out: T) = Props(classOf[WebSocketActor], out)
}

@Singleton
class WebSocketActor(out: ActorRef) extends Actor {
  override def preStart(): Unit = {
    super.preStart
    out ! OutEvent(null, JsString("Starting WebSocket Event Listener"))
  }

  def receive: Receive = {
    case in: InEvent => // out ! OutEvent("ID", 0, ("OutEvent"))
    // case _ => out ! OutEvent("", 0, "invalid request")
  }
}