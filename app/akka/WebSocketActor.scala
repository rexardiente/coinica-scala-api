package akka

import javax.inject.Singleton
import akka.actor._
import play.api.libs.json._
import models.domain.{ OutEvent, InEvent}
import models.domain.Event._

object WebSocketActor {
  def props[T <: ActorRef](out: T) = Props(classOf[WebSocketActor], out)
}

@Singleton
class WebSocketActor(out: ActorRef) extends Actor {
  override def preStart(): Unit = {
    super.preStart
    // Insert into the db active users..
    // check if succesfully inserted 
    // else send message and close connection
    if (true) {
    	// generate new session and send to user
    	// use it for the next ws request...
    	out ! OutEvent(null, Json.obj("status" -> "succes", "session" -> out.toString))
    } else {
		out ! OutEvent(null, JsString("error"))
    }
  }

  override def postStop(): Unit = {
    // Remove user from db active users..
  }

  def receive: Receive = {
    case in: InEvent =>
    	println(Json.toJson(in))
	// out ! OutEvent("ID", 0, ("OutEvent"))
    // case _ => out ! OutEvent("", 0, "invalid request")
  }
}

// Check if user session exist to use as reference for akka actor..
// save into DB for easy broadcast of messages..

// Broadcast into specific user if still active else broadcast all result. 
// Bradcast all will be the best solution for game win and lose updates
// specified broadcast if theres only changes on specific user games