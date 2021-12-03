package akka

import javax.inject.{ Singleton, Inject }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import play.api.libs.json._
import models.domain._
import models.domain.enum.VIP
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
    // update Daily Task
    case task: DailyTask =>
      dailyTask.addOrUpdate(task)
    // update Challenge Tracker
    case challenge: ChallengeTracker =>
      try {
        for {
          vipAcc <- vipRepo.findByID(challenge.user)
          vipBenefit <- vipRepo.getBenefitByID(vipAcc.map(_.rank).getOrElse(null))
          // process VIP points and VIP level..
          updatedVIPAcc <- {
            vipBenefit
              .map { bnft =>
                var vip: VIPUser = vipAcc.get
                val pointEarned: Double = (challenge.points * bnft.redemption_rate)
                val newPoints: Double = vip.points + pointEarned
                // check if has enough points for nxt rank
                if (newPoints >= bnft.points.id.toDouble) {
                  // check if has next rank, update rank lvl and assume GOLD rank will be next lvl
                  if (vip.rank != vip.next_rank)
                    vip = vip.copy(points = newPoints, rank=vip.next_rank, next_rank=VIP.GOLD)
                  else vip = vip.copy(points = newPoints, rank=vip.next_rank, next_rank=vip.next_rank)
                }
                // if no enough points for next rank, then update only points..
                else vip = vip.copy(points = newPoints)
                // update vip account
                vipRepo.update(vip).map((_, pointEarned))
              }
              .getOrElse(Future.successful((0, 0D)))
          }
          challengeUpdated <- {
            updatedVIPAcc match {
              // updated VIP User
              case (result, pointEarned) =>
                if (result > 0) dailyChallenge.addOrUpdate(challenge.copy(points = pointEarned))
                else Future.successful(0)
              // if error return 0
              case _ => Future.successful(0)
            }
          }
        } yield (challengeUpdated)
      } catch {
        case _: Throwable  => ()
      }

    case e => log.info("DynamicSystemProcessActor: invalid request")
      // out.map(_ ! OutEvent(JsNull, JsString("invalid")))
  }
}