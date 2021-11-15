package utils.auth

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalDateTime, Instant }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.mutable
import play.api.mvc._
import models.domain.UserAccount
import models.service.UserAccountService
import utils.SystemConfig.TOKEN_EXPIRATION
import AccountTokenSession.{ LOGIN => loginSession }

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
  // def transform[A](request: Request[A]): Future[SecureUserRequest[A]] = {
  //   // accService
  //   //   .getUserAccountByIDAndToken(
  //   //     request.headers.get("CLIENT_ID").map(UUID.fromString(_)).getOrElse(UUID.randomUUID),
  //   //     request.headers.get("CLIENT_TOKEN").getOrElse(null))
  //   //   .map(new SecureUserRequest(_, request))
  // }
  def transform[A](request: Request[A]): Future[SecureUserRequest[A]] = {
    val clientID: UUID = request.headers.get("CLIENT_ID").map(UUID.fromString(_)).getOrElse(UUID.randomUUID)
    val clientToken: String = request.headers.get("CLIENT_TOKEN").getOrElse(null)
    val currentTime: Long = Instant.now.getEpochSecond
    // find existing account login session based on requested CLIENT_ID and CLIENT_TOKEN
    loginSession.filter(x => x._1 == clientID && x._2._1 == clientToken && x._2._2 >= currentTime)
        .headOption
        .map(session => {
          // update existing login session then proceed on the process..
          loginSession(session._1) = (session._2._1, TOKEN_EXPIRATION)
          accService.getAccountByID(session._1).map(new SecureUserRequest(_, request))
        })
        .getOrElse(Future.successful(new SecureUserRequest(None, request)))
  }
  // def generateLoginToken[T <: UserToken](user: T) = {
  //   val token: String = s"==token${UUID.randomUUID().toString}"
  //   // limit/expire session after #minutes of creation,
  //   // else send renew session..
  //   user.copy(token=Some(token), login=Some(TOKEN_EXPIRATION))
  // }
  def generateToken(): (String, Long) = (s"==token${UUID.randomUUID().toString}", TOKEN_EXPIRATION)
}