package akka

import javax.inject.{ Inject, Singleton }
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.Logger

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  case class Connect(connection: ActorRef)
  case class Disconnect(disconnection: ActorRef)
  object Broadcast
  
  val logger: Logger = Logger(this.getClass())

  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 3.minute, interval = 3.minute) { () =>
      // the block of code that will be executed
      self ! Broadcast
    }
  }

  def receive: Receive = {
    case connect: Connect => // Indicators if there's new Client Connected..
      // println(connect)

    case Broadcast => logger.info("Executing SchedulerActor in every 3 min...")
    //   Clients.clientsList.append(connect.connection)
    //   self ! Json.obj("message"->"Connected")

    // case disconnect: Disconnect => // Indicators if there's a Client Disconnected..
    //   Clients.clientsList.find(_ == disconnect.disconnection).map(Clients.clientsList -= _)
    //   self ! Json.obj("message"->"Somebody is disconnected")

    // case json: JsValue => // Sends all the messages to all Clients..
    //   Clients.clientsList.map(_ ! json)
  }
}