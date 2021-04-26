package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }
import models.domain.UserAccount
import models.service.UserAccountService
import utils.Config

@Singleton
class EmailValidation @Inject()(accountService: UserAccountService)(implicit val ec: ExecutionContext) {
	def emailFromCode[T >: String](code: T): Future[(T, T, T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      // remove random starting random String from the code..
      val (invalidCode, validCode) = raw(0).splitAt(Config.MAIL_RANDOM_CODE_LIMIT)
      val (password, expiration): (String, String) = validCode.splitAt(64)
      val email: String = raw(1) // email
      val username: String = raw(2)

      areValidParams(username, password, expiration) match {
        case true => isAccountExists(username, password).map(exists => if (exists) (username, password, email, expiration) else null)
        case false => null
      }
    }
    catch { case _: Throwable => Future.successful(throw new IllegalArgumentException("Invalid Code")) }
  }

  def passwordFromCode[T >: String](code: T): Future[(T, T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val (invalidCode, validCode) = raw(0).splitAt(Config.MAIL_RANDOM_CODE_LIMIT)
      val (password, expiration): (String, String) = validCode.splitAt(64)
      val username: String = raw(1)
      // check if valid code or not.
      areValidParams(username, password, expiration) match {
        case true => isAccountExists(username, password).map(exists => if (exists) (username, password, expiration) else null)
        case false => null
      }
    }
    catch { case _: Throwable => throw new IllegalArgumentException("Invalid Code") }
  }

  // validate account credentials
  private def isAccountExists(u: String, p: String): Future[Boolean] = accountService.exists(u, p)
  private def areValidParams(u: String, p: String, e: String): Boolean =
    try {
      if (u.isEmpty || p.isEmpty || e.isEmpty) false
      else {
        // check if valid time
        val toInstant: Instant = Instant.ofEpochSecond(e.toLong)
        val isExpired: Boolean = toInstant.getEpochSecond >= Instant.now.getEpochSecond
        if (isExpired) true else false
      }
    } catch {
      case _: Throwable => false
    }
}