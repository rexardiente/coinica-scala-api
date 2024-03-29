package utils.auth

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import play.api.mvc._
import models.domain.UserAccount
import models.service.{ UserAccountService, PlatformConfigService }
import AccountTokenSession.{ LOGIN => loginSession }
import utils.SystemConfig.instantNowUTC

@Singleton
class SecureUserRequest[A](val account: Option[UserAccount], request: Request[A]) extends WrappedRequest[A](request)
@Singleton
class SecureUserAction @Inject()(
														val parser: BodyParsers.Default,
														configService: PlatformConfigService,
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
  private def getTokenExpiration(): Future[Long] = {
    configService
      .getTokenExpiration()
      .map(DEFAULT_TOKEN_EXPIRATION => (instantNowUTC().getEpochSecond + (DEFAULT_TOKEN_EXPIRATION * 60)).toLong)
  }
  def transform[A](request: Request[A]): Future[SecureUserRequest[A]] = {
    val clientID: UUID = request.headers.get("CLIENT_ID").map(UUID.fromString(_)).getOrElse(UUID.randomUUID)
    val clientToken: String = request.headers.get("CLIENT_TOKEN").getOrElse(null)

    for {
      TOKEN_EXPIRATION <- getTokenExpiration()
      isAuthenticated <- {
        loginSession
          .find(x => x._1 == clientID && x._2._1 == clientToken)
          .map(session => {
            val currentTime: Long = instantNowUTC().getEpochSecond
            if (session._2._2 >= currentTime) {
              // update existing login session then proceed on the process..
              loginSession(session._1) = (session._2._1, TOKEN_EXPIRATION)
              accService.getAccountByID(session._1).map(new SecureUserRequest(_, request))
            }
            else {
              loginSession.remove(session._1)
              Future.successful(new SecureUserRequest(None, request))
            }
          })
          .getOrElse(Future.successful(new SecureUserRequest(None, request)))
      }
    } yield (isAuthenticated)
  }
  // def generateLoginToken[T <: UserToken](user: T) = {
  //   val token: String = s"==token${UUID.randomUUID().toString}"
  //   // limit/expire session after #minutes of creation,
  //   // else send renew session..
  //   user.copy(token=Some(token), login=Some(TOKEN_EXPIRATION))
  // }
  def generateToken(): Future[(String, Long)] = {
    getTokenExpiration.map(TOKEN_EXPIRATION => (s"==token${UUID.randomUUID().toString}", TOKEN_EXPIRATION))
  }
}