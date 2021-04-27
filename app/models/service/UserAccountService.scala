package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneId, ZoneOffset }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain.{ PaginatedResult, UserAccount, VIPUser, UserTokens }
import models.repo.{ UserAccountRepo, VIPUserRepo, UserTokensRepo }

@Singleton
class UserAccountService @Inject()(userAccountRepo: UserAccountRepo, vipUserRepo: VIPUserRepo, userTokensRepo: UserTokensRepo) {
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

  def getAccountByEmailAddress(email: String): Future[Option[UserAccount]] =
    userAccountRepo.getAccountByEmailAddress(email)

  def getAccountByUserNamePassword(user: String, pass: String): Future[Option[UserAccount]] =
  	userAccountRepo.getAccountByUserNamePassword(user, pass)

  def newUserAcc(acc: UserAccount): Future[Int] =
  	userAccountRepo.add(acc)

  def exists(username: String, password: String): Future[Boolean] =
    userAccountRepo.exist(username, password)

  def isEmailExist(email: String): Future[Boolean] =
    userAccountRepo.isEmailExist(email)

  def addOrUpdateEmailAccount(id: UUID, email: String): Future[Int] = {
    for {
      isExists <- isEmailExist(email)
      // get acccount based on first validations, proceed adding or updating email
      account <- getAccountByID(id)
      process <- {
        // check if email not associated with any accounts and account exists
        if (!isExists && account != None) {
          try {
            val updatedAccount: UserAccount = account.get.copy(email = Some(email), isVerified = true)
            updateUserAccount(updatedAccount)
          } catch {
            case _: Throwable => Future(0)
          }
        }
        else Future(0)
      }
    } yield (process)
  }

  def newVIPAcc(vip: VIPUser): Future[Int] =
  	vipUserRepo.add(vip)

  def getUserAccountBySessionToken(token: String): Future[Option[UserAccount]] = {
    for {
      // check if token exists on DB..
       hasValidToken <- userTokensRepo.getLoginByToken(token)
       // validate
       processed <- {
         if (hasValidToken != None) getAccountByID(hasValidToken.map(_.id).getOrElse(UUID.randomUUID))
         else Future(None)
       }
    } yield (processed)
  }
  def updateUserToken(user: UserTokens): Future[Int] =
    userTokensRepo.update(user)
  def getUserTokenByID(id: UUID): Future[Option[UserTokens]] =
    userTokensRepo.getByID(id)

  def addUpdateUserToken(user: UserTokens): Future[Int] = {
    for {
      exists <- userTokensRepo.exists(user.id)
      process <- {
        if (exists) userTokensRepo.update(user)
        else userTokensRepo.add(user)
      }
    } yield (process)
  }
  def removePasswordTokenByID(id: UUID): Future[Int] =
    for {
      token <- userTokensRepo.getByID(id)
      process <- {
        if (token != None) userTokensRepo.update(token.get.copy(password = None))
        else Future(0)
      }
    } yield (process)
  def removeEmailTokenByID(id: UUID): Future[Int] =
    for {
      token <- userTokensRepo.getByID(id)
      process <- {
        if (token != None) userTokensRepo.update(token.get.copy(email = None))
        else Future(0)
      }
    } yield (process)

}