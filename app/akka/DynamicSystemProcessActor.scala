package akka

import javax.inject.{ Singleton, Inject }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.repo._
import akka.common.objects._

// Responsible for create and update system functionality for
// ranking, challenge, task and VIP
object DynamicSystemProcessActor {
  def props(dailyTask: DailyTaskRepo,
            dailyChallenge: ChallengeTrackerRepo,
            vipRepo: VIPUserRepo
            )(implicit system: ActorSystem) =
      Props(classOf[DynamicSystemProcessActor], dailyTask, dailyChallenge, vipRepo, system)
}

@Singleton
class DynamicSystemProcessActor@Inject()(
        dailyTask: DailyTaskRepo,
        dailyChallenge: ChallengeTrackerRepo,
        vipRepo: VIPUserRepo)(implicit system: ActorSystem) extends Actor {
  private val log: LoggingAdapter = Logging(context.system, this)

  override def preStart(): Unit = {
    super.preStart
    log.info(s"DynamicSystemProcessActor Actor Initialized")
  }

  override def postStop(): Unit = {}

  def receive: Receive = {
    case task: DailyTask =>
      dailyTask.addOrUpdate(task)

    case challenge: ChallengeTracker =>
      try {
        for {
          vipAcc <- vipRepo.findByID(challenge.user)
          vipBnft <- vipRepo.getBenefitByID(vipAcc.get.rank)
          _ <- Future.successful {
            val bnft: VIPBenefit = vipBnft.get
            // new earned points * redemption rate
            val newPoints = challenge.points * bnft.redemption_rate
            // create new updated VIP User
            dailyChallenge.addOrUpdate(challenge.copy(points = newPoints))
          }
        } yield ()
      } catch {
        case _: Throwable  => ()
      }

    case e => log.info("DynamicSystemProcessActor: invalid request")
      // out.map(_ ! OutEvent(JsNull, JsString("invalid")))
  }
}