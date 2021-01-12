package akka

import javax.inject.Singleton
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain.{ Event, OutEvent, InEvent }
import models.domain.Event._

object WebSocketActor {
  def props[T <: ActorRef](out: T) = Props(classOf[WebSocketActor], out)
}

@Singleton
class WebSocketActor(out: ActorRef) extends Actor {
  val code: Int = out.hashCode
  val log: LoggingAdapter = Logging(context.system, this)
  override def preStart(): Unit = {
    super.preStart
    // Insert into the db active users..
    // check if succesfully inserted
    // else send message and close connection
  	// generate new session and send to user
  	// use it for the next ws request...
  	out ! OutEvent(JsNull, JsString("connected"))
    log.info(s"${code} ~> WebSocket Actor Initialized")
  }

  override def postStop(): Unit = {
    // Remove user from db active users..
    log.error(s"${code} ~> disconnected")
  }

  def receive: Receive = {
    case ev: Event =>
      // TODO: check if user is subscribed else do not allow..
      ev match {
        case in: InEvent =>
          log.info(in.toJson.toString)
          out ! OutEvent(JsString(code.toString), JsString("subscribed"))

        case oe: OutEvent =>
          log.info(oe.toJson.toString)

        case _ =>
          log.info("Unknown")
      }

    case _ => out ! OutEvent(JsNull, JsString("Invalid Request"))
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