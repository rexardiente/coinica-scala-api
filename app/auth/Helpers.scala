package auth.helpers

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalDateTime, Instant }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.mutable
import models.domain.UserAccount
import models.repo.UserAccountRepo
import play.api.mvc._

@Singleton
class SecureUserRequest[A](val account: Option[UserAccount], request: Request[A]) extends WrappedRequest[A](request)
@Singleton
class SecureUserAction @Inject()(
														val parser: BodyParsers.Default,
														accRepo: UserAccountRepo
													)(implicit val executionContext: ExecutionContext)
											  	extends ActionBuilder[SecureUserRequest, AnyContent]
											    with ActionTransformer[Request, SecureUserRequest] {
  // get by session token but make sure token is not expired else return false.
  def transform[A](request: Request[A]) = {
    accRepo
      .getBySessionToken(request.headers.get("EGS_TOKEN_SESSION").getOrElse(null))
      .map(new SecureUserRequest(_, request))
  }
  def generateToken(account: UserAccount): UserAccount = {
    val token: String = s"==token${UUID.randomUUID().toString}"
    val sessionTime: Long = Instant.now.getEpochSecond + (60 * 5)
    // limit/expire session after 5 minutes of creation, else send renew session..
    account.copy(token = Some(token), tokenLimit = Some(sessionTime))
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
