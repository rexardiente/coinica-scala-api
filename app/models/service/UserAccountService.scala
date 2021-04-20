package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneId, ZoneOffset }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain.{ PaginatedResult, UserAccount, VIPUser }
import models.repo.{ UserAccountRepo, VIPUserRepo }

@Singleton
class UserAccountService @Inject()(userAccountRepo: UserAccountRepo, vipUserRepo: VIPUserRepo) {
  def isExist(name: String): Future[Boolean] =
  	userAccountRepo.exist(name)

  def getAccountByID(id: UUID): Future[Option[UserAccount]] =
  	userAccountRepo.getByID(id)

  def getAccountByName(name: String): Future[Option[UserAccount]] =
  	userAccountRepo.getByName(name)

  def updateUserAccount(acc: UserAccount): Future[Int] =
  	userAccountRepo.update(acc)

  def getAccountByCode(code: String): Future[Option[UserAccount]] =
  	userAccountRepo.getAccountByReferralCode(code)

  def getAccountByUserNamePassword(user: String, pass: String): Future[Option[UserAccount]] =
  	userAccountRepo.getAccountByUserNamePassword(user, pass)

  def newUserAcc(acc: UserAccount): Future[Int] =
  	userAccountRepo.add(acc)

  def newVIPAcc(vip: VIPUser): Future[Int] =
  	vipUserRepo.add(vip)
}