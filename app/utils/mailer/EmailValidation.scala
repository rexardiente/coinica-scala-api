package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import models.domain.UserAccount
import models.service.UserAccountService
import utils.Config
import utils.auth.AccountTokenSession.{ RESET_EMAIL, RESET_PASSWORD }

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

  def resetPasswordFromCode(id: UUID, code: String): Future[Option[UUID]] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val token: String = raw(0)
      val expiration: Long = raw(1).toLong
      // check if valid code or not
      for {
        hasSession <- Future.successful(RESET_EMAIL.filter(_._2 == (token, expiration)).headOption)
        isValidCode <- Future.successful {
          hasSession
            .map { session =>
              if (session._2._2 >= Instant.now.getEpochSecond) Some(id)
              else None
            }
            .getOrElse(None)
        }
      } yield (isValidCode)
    }
    catch { case _: Throwable => Future.successful(None) }
  }
  // validate account credentials
  private def isAccountExists(u: String, p: String): Future[Boolean] = accountService.exists(u, p)
  private def hasAccount(u: String, p: String): Future[Option[UserAccount]] = accountService.getAccountByUserNamePassword(u, p)
}