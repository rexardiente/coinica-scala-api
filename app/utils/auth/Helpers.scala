package utils.auth

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalDateTime, Instant }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.mutable
import play.api.mvc._
import models.domain.{ UserAccount, UserToken }
import models.service.UserAccountService
import utils.Config

@Singleton
class SecureUserRequest[A](val account: Option[UserAccount], request: Request[A]) extends WrappedRequest[A](request)
@Singleton
class SecureUserAction @Inject()(
														val parser: BodyParsers.Default,
														accService: UserAccountService,
                            implicit val executionContext: ExecutionContext)
													extends ActionBuilder[SecureUserRequest, AnyContent]
											    with ActionTransformer[Request, SecureUserRequest] {
  // get by session token but make sure token is not expired else return false.
  def transform[A](request: Request[A]) = {
    accService
      .getUserAccountBySessionToken(request.headers.get("EGS_TOKEN_SESSION").getOrElse(null))
      .map(new SecureUserRequest(_, request))
  }

  def generateLoginToken(user: UserToken): UserToken = {
    val token: String = s"==token${UUID.randomUUID().toString}"
    // limit/expire session after 5 minutes of creation, else send renew session..
    user.copy(token=Some(token), login=Some(Config.MAIL_EXPIRATION))
  }
}

// case class UserAccountSession(token: String, username: String, expiration: LocalDateTime)
// object SecureUserAction {
  // generate or replace new token
  // Map token -> UserAccountSession
  // private val sessions = mutable.Map.empty[String, UserAccountSession]
  // def getSession(token: String): Option[UserAccountSession] = {
  //   sessions.get(token)
  // }
  // def generateToken(username: String): String = {
  //   // we use UUID to make sure randomness and uniqueness on tokens
  //   val token = s"token-${UUID.randomUUID().toString}"
  //   sessions.put(token, UserAccountSession(token, username, LocalDateTime.now().plusMinutes(5)))
  //   token
  // }
// }
