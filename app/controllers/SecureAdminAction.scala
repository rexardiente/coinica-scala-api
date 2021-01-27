package utils.auth

import java.util.UUID
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.Exception.catching
import play.api.mvc._
import play.api.mvc.Results
import cats.data.OptionT
import cats.implicits._
import models.domain.{ Admin, Security }
import models.domain.enum.Roles
import models.repo.AdminRepo

case class SecureAdmin[A](
		val admin: Admin,
		val security: Security,
		request: Request[A])
	extends WrappedRequest[A](request)

class SecureAdminAction @Inject()(
    protected val AdminRepo: AdminRepo,
    val parser: BodyParsers.Default,
    implicit val executionContext: ExecutionContext)()
 	extends ActionBuilder[SecureAdmin, AnyContent] {
  self =>
  val key = "aid" // Admin ID

  private def onUnauthorized[A](request: Request[A]): Future[Result] = {
    Future.successful(Results.Unauthorized)
  }

  private def onForbidden[A](request: Request[A]): Future[Result] = {
    Future.successful(Results.Forbidden)
  }

  private final def authenticate[A](request: Request[A]): OptionT[Future, SecureAdmin[A]] = {
    OptionT.fromOption[Future](request.session.get(key))
      .subflatMap(id => catching(classOf[IllegalArgumentException]).opt(UUID.fromString(id)))
      .flatMap(AdminRepo.find(_).map { case (teacher, security) =>
        SecureAdmin(teacher, security, request)
      })
  }

  final def invokeBlock[A](request: Request[A], block: (SecureAdmin[A]) => Future[Result]): Future[Result] = {
    authenticate(request)
      .semiflatMap { r =>
        if (r.security.disabledAt.isEmpty) block(r)
        else onForbidden(r)
      }
      .getOrElseF(onUnauthorized(request))
  }

  final def hasAny(roles: Roles.Value*) = new ActionBuilder[SecureAdmin, AnyContent] {
    implicit val executionContext: ExecutionContext = self.executionContext
    val parser: BodyParsers.Default = self.parser
    def invokeBlock[A](request: Request[A], block: (SecureAdmin[A]) => Future[Result])
      : Future[Result] = {
      authenticate(request)
        .semiflatMap { r =>
          if (roles.exists(_ == r.admin.role) && r.security.disabledAt.isEmpty) block(r)
          else onForbidden(r)
        }
        .getOrElseF(onUnauthorized(request))
      }
  }

  final def noAuth = new ActionBuilder[SecureAdmin, AnyContent] {
    implicit val executionContext: ExecutionContext = self.executionContext
    val parser: BodyParsers.Default = self.parser
    def invokeBlock[A](request: Request[A], block: (SecureAdmin[A]) => Future[Result])
      : Future[Result] =
      authenticate(request).semiflatMap(r => block(r)).getOrElseF(onUnauthorized(request))
  }


}