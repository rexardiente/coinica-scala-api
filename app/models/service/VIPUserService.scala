package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import play.api.libs.json._
import models.domain.VIPUser
import models.repo.{ ChallengeTrackerRepo, VIPUserRepo }
import models.domain.enum.VIPBenefitPoints

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
        hasTracker
          .map { tracker =>
            current.map(vip => vip.copy(points = (vip.points + tracker.points), payout = (vip.payout + tracker.payout)))
          }.getOrElse(current)
      }
      _ <- Future.successful {
        updated.map { vip =>
          val pointsToInt: Int = vip.points.round.toInt
          for {
            vipBenefit <- vipRepo.getBenefitByID(vip.rank)
            newJSON <- Future.successful {
              vipBenefit
                .map { bnft =>
                  val currentLvlMax: Double = {
                    if (0 to VIPBenefitPoints.BRONZE.id contains pointsToInt)
                      VIPBenefitPoints.BRONZE.id
                    else if ((VIPBenefitPoints.BRONZE.id + 1) to VIPBenefitPoints.SILVER.id contains pointsToInt)
                      VIPBenefitPoints.SILVER.id
                    else VIPBenefitPoints.GOLD.id }.toDouble

                  val prevLvlMax: Double = {
                    if (0 to VIPBenefitPoints.BRONZE.id contains pointsToInt) 0
                    else if ((VIPBenefitPoints.BRONZE.id + 1) to VIPBenefitPoints.SILVER.id contains pointsToInt)
                      VIPBenefitPoints.BRONZE.id
                    else VIPBenefitPoints.SILVER.id }.toDouble

                  val hasPrevLvl: Double = vip.points - prevLvlMax
                  val dividend: Double = hasPrevLvl / currentLvlMax
                  vip.toJson((dividend * 100).toInt)
                }
                .getOrElse(None)
            }
          } yield (newJSON)
        }
        .getOrElse(0)
      }
    } yield (updated)
  }
}