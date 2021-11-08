package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import models.domain.UserAccount
import models.service.UserAccountService
import utils.Config
import utils.auth.AccountTokenSession.{ RESET_PASSWORD, ADD_OR_RESET_EMAIL }

@Singleton
class EmailValidation @Inject()(accountService: UserAccountService)(implicit val ec: ExecutionContext) {
	def addOrUpdateEmailFromCode(id: UUID, code: String): Boolean = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val token: String = raw(0)
      val expiration: Long = raw(1).toLong
      // check if valid code or not
      val hasSession = ADD_OR_RESET_EMAIL.filter(x => x._1 == id && x._2 == (token, expiration)).headOption

      hasSession.map(session => if (session._2._2 >= Instant.now.getEpochSecond) true else false).getOrElse(false)
    }
    catch { case _: Throwable => false }
  }

  def resetPasswordFromCode(id: UUID, code: String): Boolean = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val token: String = raw(0)
      val expiration: Long = raw(1).toLong
      // check if valid code or not
      val hasSession = RESET_PASSWORD.filter(x => x._1 == id && x._2 == (token, expiration)).headOption
      hasSession.map(session => if (session._2._2 >= Instant.now.getEpochSecond) true else false).getOrElse(false)
    }
    catch { case _: Throwable => false }
  }
  // validate account credentials
  private def isAccountExists(u: String, p: String): Future[Boolean] = accountService.exists(u, p)
  private def hasAccount(u: String, p: String): Future[Option[UserAccount]] = accountService.getAccountByUserNamePassword(u, p)
}