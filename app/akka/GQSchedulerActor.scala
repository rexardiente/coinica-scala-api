package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ concurrent, UUID }
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset, ZoneId }
import scala.util.{ Success, Failure }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import models.domain._
import models.domain.eosio._
import models.service._
import utils.lib._
// import akka.common.objects._
import utils.Config._

object GQSchedulerActor {
  val defaultTimeSet: Int            = GQ_DEFAULT_BATTLE_TIMER
  val scheduledTime : FiniteDuration = { defaultTimeSet }.minutes
  var isIntialized  : Boolean        = false
  def props(historyService: HistoryService,
            userAccountService: UserAccountService,
            ghostQuestGameService: GhostQuestGameService)(implicit system: ActorSystem) =
    Props(classOf[GQSchedulerActor],
          historyService,
          userAccountService,
          ghostQuestGameService,
          system)
}

@Singleton
class GQSchedulerActor @Inject()(
      historyService: HistoryService,
      userAccountService: UserAccountService,
      ghostQuestGameService: GhostQuestGameService,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
    )(implicit system: ActorSystem ) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, concurrent.TimeUnit.SECONDS)

  override def preStart: Unit = {
    super.preStart
    // keep alive connection
    // akka.stream.scaladsl.Source.tick(0.seconds, 60.seconds, "GQSchedulerActor").runForeach(n => ())
    system.actorSelection("/user/GQSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!GQSchedulerActor.isIntialized) {
          GQSchedulerActor.isIntialized = true
          log.info("GQ Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
  	case "REQUEST_BATTLE_NOW" =>
  		for {
  			// clean characters DB
  			_ <- Future(())
  		} yield ()
  		// get all characters on smartcontract, save to DB..
  		val allCharacters: Future[Option[Seq[GhostQuestTableGameData]]] = ghostQuestGameService.getAllCharacters

		case _ =>
	}
}