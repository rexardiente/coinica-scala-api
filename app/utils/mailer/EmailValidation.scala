package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }
import models.domain.UserAccount
import models.service.UserAccountService
import utils.Config

@Singleton
class EmailValidation @Inject()(accountService: UserAccountService)(implicit val ec: ExecutionContext) {
	def emailFromCode[T >: String](code: T): Future[(T, T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      // remove random starting random String from the code..
      val (password, invalidCode) = raw(0).splitAt(64)
      val email: String = raw(1) // email
      val username: String = raw(2)

      for {
        account <- hasAccount(username, password)
        tokenSession <- account.map(acc => accountService.getUserTokenByID(acc.id)).getOrElse(Future(None))
        result <- Future.successful {
          tokenSession
            .map { session =>
              if (session.email.map(_ >= Instant.now.getEpochSecond).getOrElse(false)) (username, password, email)
              else null
            }.getOrElse(null)
        }
      } yield (result)
    }
    catch { case _: Throwable => Future.successful(throw new IllegalArgumentException("Invalid Code")) }
  }

  def passwordFromCode[T >: String](code: T): Future[(T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val (password, invalidCode) = raw(0).splitAt(64)
      val username: String = raw(1)
      // check if valid code or not.
      // hasAccount(username, password).map(exists => if (exists) (username, password) else null)
      for {
        account <- hasAccount(username, password)
        tokenSession <- account.map(acc => accountService.getUserTokenByID(acc.id)).getOrElse(Future(None))
        result <- Future.successful {
          tokenSession
            .map { session =>
              if (session.password.map(_ >= Instant.now.getEpochSecond).getOrElse(false)) (username, password)
              else null
            }.getOrElse(null)
        }
      } yield (result)
    }
    catch { case _: Throwable => throw new IllegalArgumentException("Invalid Code") }
  }
  // validate account credentials
  private def isAccountExists(u: String, p: String): Future[Boolean] = accountService.exists(u, p)
  private def hasAccount(u: String, p: String): Future[Option[UserAccount]] = accountService.getAccountByUserNamePassword(u, p)
}