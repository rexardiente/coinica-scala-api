package utils.auth

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalDateTime, Instant }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.mutable
import play.api.mvc._
import models.domain.{ UserAccount, UserToken }
import models.service.UserAccountService
import utils.Config.TOKEN_EXPIRATION

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
  def transform[A](request: Request[A]): Future[SecureUserRequest[A]] = {
    accService
      .getUserAccountByIDAndToken(
        request.headers.get("CLIENT_ID").map(UUID.fromString(_)).getOrElse(UUID.randomUUID),
        request.headers.get("CLIENT_TOKEN").getOrElse(null))
      .map(new SecureUserRequest(_, request))
  }

  def generateLoginToken[T <: UserToken](user: T) = {
    val token: String = s"==token${UUID.randomUUID().toString}"
    // limit/expire session after #minutes of creation,
    // else send renew session..
    user.copy(token=Some(token), login=Some(TOKEN_EXPIRATION))
  }
}