package akka

import javax.inject.{ Inject, Singleton }
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.Logger

object SchedulerActor {
  def props = Props[SchedulerActor]
  case class Connect(connection: ActorRef)
  case class Disconnect(disconnection: ActorRef)
  case class Broadcast(connection: ActorRef)
}

@Singleton
class SchedulerActor @Inject()(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  val logger: Logger = Logger(this.getClass())

  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // the block of code that will be executed
    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 3.minute, interval = 3.minute) { () =>
      self ! "Executing SchedulerActor in every 3 min..."
    }
  }

  def receive: Receive = {
    case connect: SchedulerActor.Connect => // Indicators if there's new Client Connected..
      // println(connect)

    case str: String => logger.info(str)
      // logger.info(str.toString)
    //   Clients.clientsList.append(connect.connection)
    //   self ! Json.obj("message"->"Connected")

    // case disconnect: Disconnect => // Indicators if there's a Client Disconnected..
    //   Clients.clientsList.find(_ == disconnect.disconnection).map(Clients.clientsList -= _)
    //   self ! Json.obj("message"->"Somebody is disconnected")

    // case json: JsValue => // Sends all the messages to all Clients..
    //   Clients.clientsList.map(_ ! json)
  }
}