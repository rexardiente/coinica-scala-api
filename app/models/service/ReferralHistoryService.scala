package models.service

import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.{ Instant, ZoneId }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import models.domain.{ PaginatedResult, ReferralHistory, UserAccount, VIPUser, VIPBenefit }
import models.repo.{ UserAccountRepo, ReferralHistoryRepo, VIPUserRepo }
import models.domain.enum._

@Singleton
class ReferralHistoryService @Inject()(
  userAccountRepo: UserAccountRepo,
  referralRepoHistory: ReferralHistoryRepo,
  vipUserRepo: VIPUserRepo) {
  def paginatedResult[T >: ReferralHistory](limit: Int, offset: Int): Future[PaginatedResult[T]] = {

	  for {
      tasks <- referralRepoHistory.findAll(limit, offset)
      size <- referralRepoHistory.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(tasks.size, tasks.toList, hasNext)
  }

  def getByCode(code: String): Future[Seq[ReferralHistory]] =
    for {
      // check if user exists else return seq empty
      isExists <- userAccountRepo.isCodeExist(code)
      result <- if (isExists) referralRepoHistory.getByCode(code) else Future(Seq.empty)
    } yield result
  // check if user exists and not referred by someone
  // make sure code doesn't belong to itself
  def applyReferralCode(appliedBy: UUID, code: String): Future[Int] = {
    for {
      isCodeOwnedBySelf <- userAccountRepo.isCodeOwnedBy(appliedBy, code)
      getAccByReferralCode <- userAccountRepo.getAccountByReferralCode(code)
      hasNoReferral <- userAccountRepo.hasNoReferral(appliedBy)
      // check if account referred by this account..
      isAlreadyReferred <- referralRepoHistory.isReferred(appliedBy, code)
      result <- {
        // code doesn't belong to itself and no existing claimed referral code
        if (!isCodeOwnedBySelf && hasNoReferral != None && getAccByReferralCode != None && !isAlreadyReferred ) {
          try {
            // update each accounts referral statuses..
            // get account referrer
            val referrer: UserAccount = getAccByReferralCode.get
            val referred: UserAccount = hasNoReferral.get
            // if referrer has no yet been claimed, and to claime one then
            // must not allowed to claimed from those he referred
            referralRepoHistory.isReferred(referrer.id, referred.referral_code).map { isAccReferrer =>
              if (!isAccReferrer) {
                for {
                  _ <- {
                    // get VIP Benefits based on account VIP rank.. (Referrer)
                    for {
                      vipAccount <- vipUserRepo.findByID(referrer.id)
                      vipBenefit <- vipUserRepo.getBenefitByID(vipAccount.map(_.rank).getOrElse(VIP.BRONZE))
                      // check if all validations are true
                      _ <- Future.successful {
                        if (vipAccount != None && vipBenefit != None) {
                          val getVIPAccount: VIPUser = vipAccount.get
                          val getVIPBenefit: VIPBenefit = vipBenefit.get
                          val newReferralCount = getVIPAccount.referral_count + 1
                          // update VIP acc referral count
                          Await.ready(vipUserRepo.update(getVIPAccount.copy(referral_count = newReferralCount)), Duration.Inf)
                          // update User acc referral referral amount
                          Await.ready(userAccountRepo.update(referrer.copy(referral = getVIPBenefit.referral_rate * newReferralCount)), Duration.Inf)
                        }
                      }
                    } yield ()
                  }
                  _ <- {
                    // get VIP Benefits based on account VIP rank.. (referred)
                    // (VIP BRONZE VALUE / 2) = referral ammount claimed by referred account
                    for {
                      vipAccount <- vipUserRepo.findByID(referred.id)
                      vipBenefit <- vipUserRepo.getBenefitByID(vipAccount.map(_.rank).getOrElse(VIP.BRONZE))
                      // check if all validations are true
                      _ <- Future.successful {
                        if (vipAccount != None && vipBenefit != None) {
                          val getVIPAccount: VIPUser = vipAccount.get
                          val getVIPBenefit: VIPBenefit = vipBenefit.get

                          userAccountRepo.update(referred.copy(referral = getVIPBenefit.referral_rate, referred_by = Some(code)))
                          // userAccountRepo.update(referred.copy(referral = (VIPBenefitAmount.BRONZE.id / 2), referred_by = Some(code)))
                        }
                      }
                    } yield ()
                  }
                  _ <- referralRepoHistory.add(new ReferralHistory(UUID.randomUUID, code, appliedBy, Instant.now))
                } yield ();
                (1)
              }
              else (0)
            }
          } catch { case _: Throwable => Future(0) }
        }
        else Future(0)
      }
    } yield (result)
  }
}