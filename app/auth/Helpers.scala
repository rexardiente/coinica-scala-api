package auth.helpers

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalDateTime, ZoneOffset }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.mutable
import models.domain.Account
import models.repo.AccountRepo
import play.api.mvc._

@Singleton
class UserRequest[A](val user: Option[Account], request: Request[A]) extends WrappedRequest[A](request)
case class UserAccountSession(token: String, username: String, expiration: LocalDateTime)

object UserAccountSession {
  // Map token -> UserAccountSession
  private val sessions = mutable.Map.empty[String, UserAccountSession]

  def getSession(token: String): Option[UserAccountSession] = {
    sessions.get(token)
  }

  def generateToken(username: String): String = {
    // we use UUID to make sure randomness and uniqueness on tokens
    val token = s"token-$username-${UUID.randomUUID().toString}"
    sessions.put(token, UserAccountSession(token, username, LocalDateTime.now().plusMinutes(5)))

    token
  }

}

@Singleton
class UserAction @Inject()(
														val parser: BodyParsers.Default,
														accRepo: AccountRepo
													)(implicit val executionContext: ExecutionContext)
											  	extends ActionBuilder[UserRequest, AnyContent]
											    with ActionTransformer[Request, UserRequest] {

  def transform[A](request: Request[A]) = {
    val sessionTokenOpt = request.session.get("EGS_ACCOUNT_TOKEN")
		for {
			getSession <- Future.successful {
				sessionTokenOpt
	        .flatMap(token => UserAccountSession.getSession(token))
	        .filter(_.expiration.isAfter(LocalDateTime.now()))
			}
			isExists <- accRepo.findByUserName(getSession.map(_.username).getOrElse(null))
			process <- Future.successful(new UserRequest(isExists, request))
		} yield (process)
  }
}
