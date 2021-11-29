package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import play.api.libs.json._
import models.domain.VIPUser
import models.repo.{ ChallengeTrackerRepo, VIPUserRepo }

@Singleton
class VIPUserService @Inject()(vipRepo: VIPUserRepo, chTkRepo: ChallengeTrackerRepo) {
  // calculate real-time VIP points progress
  def realtimeVipPoints(id: UUID): Future[Option[VIPUser]] = {
    for {
      // get current user VIP data
      current <- vipRepo.findByID(id)
      // get all points accumulated in daily challenge tracker
      hasTracker <- chTkRepo.findUserByID(id)
      // add the newly accumulated points into the existing one..
      updated <- Future.successful {
        hasTracker.map(tracker => current.map(vip => vip.copy(points = (vip.points + tracker.points)))).getOrElse(current)
      }
    } yield (updated)
  }
}