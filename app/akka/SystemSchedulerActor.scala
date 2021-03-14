package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ LocalTime, Instant }
import scala.util._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import models.domain.OutEvent

object SystemSchedulerActor {
  var isIntialized: Boolean = false
  def props(implicit system: ActorSystem) = Props(classOf[SystemSchedulerActor], system)
}

@Singleton
class SystemSchedulerActor @Inject()(implicit system: ActorSystem) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
  private val dynamicBroadcast: ActorRef = system.actorOf(Props(classOf[DynamicBroadcastActor], None, system))

  override def preStart: Unit = {
    super.preStart
    // check if intializer is the SchedulerActor module..
    system.actorSelection("/user/SystemSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!SystemSchedulerActor.isIntialized) {
          // 24hrs Scheduler at 6:00AM in the morning daily.. (Platform ranking calculations)
          // val dailySchedInterval: FiniteDuration = 24.hours
          // val dailySchedDelay   : FiniteDuration = {
          //     val time = LocalTime.of(17, 0).toSecondOfDay
          //     val now = LocalTime.now().toSecondOfDay
          //     val fullDay = 60 * 60 * 24
          //     val difference = time - now
          //     if (difference < 0) {
          //       fullDay + difference
          //     } else {
          //       time - now
          //     }
          //   }.seconds
          // system.scheduler.scheduleAtFixedRate(dailySchedDelay, dailySchedInterval)(() => self ! DailyScheduler)
          // set true if actor already initialized
          SystemSchedulerActor.isIntialized = true
          log.info("System Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
    case _ =>
  }
}