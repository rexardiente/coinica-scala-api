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
class UserRequest[A](val user: Option[UserAccount], request: Request[A]) extends WrappedRequest[A](request)
// case class UserAccountSession(token: String, username: String, expiration: LocalDateTime)
object UserAction {
	// generate or replace new token
  def generateToken(user: UserAccount): UserAccount = {
  	val token = s"==token${UUID.randomUUID().toString}"
  	user.copy(token = Some(token), tokenLimit = Some(Instant.now.getEpochSecond + (60 * 10)))
  }
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
}

@Singleton
class UserAction @Inject()(
														val parser: BodyParsers.Default,
														accRepo: UserAccountRepo
													)(implicit val executionContext: ExecutionContext)
											  	extends ActionBuilder[UserRequest, AnyContent]
											    with ActionTransformer[Request, UserRequest] {

  def transform[A](request: Request[A]) = {
    val sessionTokenOpt = request.headers.get("EGS_ACCOUNT_TOKEN")
		for {
			// get by session token but make sure token is not expired else return false.
			session <- accRepo.getBySessionToken(sessionTokenOpt.getOrElse(null))
			process <- Future.successful(new UserRequest(session, request))
		} yield (process)
  }
}
