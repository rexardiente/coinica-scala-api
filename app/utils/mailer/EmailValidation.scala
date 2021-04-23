package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import models.domain.UserAccount
import models.service.UserAccountService

@Singleton
class EmailValidation @Inject()(accountService: UserAccountService)(implicit val ec: ExecutionContext) {
	def emailFromCode[T >: String](code: T): Future[(T, T, T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val (password, expiration): (String, String) = raw(0).splitAt(64)
      val email: String = raw(1) // email
      val username: String = raw(2)

      isAccountExists(username, password).map { isExists =>
        if (isExists) (username, password, email, expiration)
        else throw new IllegalArgumentException("Invalid Code")
      }
    }
    catch { case _: Throwable => Future.successful(throw new IllegalArgumentException("Invalid Code")) }
  }

  def passwordFromCode[T >: String](code: T): Future[(T, T, T)] = {
    try {
      val raw: List[String] = code.toString.split("_").toList
      val (password, expiration): (String, String) = raw(0).splitAt(64)
      val username: String = raw(2)

      isAccountExists(username, password).map { isExists =>
        if (isExists) (username, password, expiration)
        else throw new IllegalArgumentException("Invalid Code")
      }
    }
    catch { case _: Throwable => throw new IllegalArgumentException("Invalid Code") }
  }

  // validate account credentials
  private def isAccountExists(u: String, p: String): Future[Boolean] = accountService.exists(u, p)

}