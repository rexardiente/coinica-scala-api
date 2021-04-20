package controllers

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDateTime }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import models.domain._
import models.repo._
import models.service._
import auth.helpers._

@Singleton
class SecureActionController @Inject()(
                          accountService: UserAccountService,
                          userAction: UserAction,
                          referralHistory:  ReferralHistoryService,
                          cc: ControllerComponents,
                        ) extends AbstractController(cc) {
  private def referralForm = Form(tuple(
    "code" -> nonEmptyText,
    "applied_by" -> uuid))

  // http://127.0.0.1:9000/donut/api/v1/token/renew
  def renewSessionToken() = userAction.async { implicit request =>
    request
      .user
      .map { user =>
        val newUserToken: UserAccount = UserAction.generateToken(user)
        accountService
          .updateUserAccount(newUserToken.copy(lastSignIn = Instant.now))
          .map { x =>
            if (x > 0) Ok(Json.obj("token" -> newUserToken.token, "limit" -> newUserToken.tokenLimit))
            else InternalServerError
          }
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def applyReferralCode() = userAction.async { implicit request =>
    request
      .user
      .map { user =>
        referralForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Form Validation Error.")),
        { case (code, appliedBy)  =>
          try {
            referralHistory
              .applyReferralCode(appliedBy, code)
              .map(r => if(r < 1) InternalServerError else Created )
          } catch {
            case _: Throwable => Future(InternalServerError)
          }
        })
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }


  // temporary add Account... http://127.0.0.1:9000/donut/api/v1/account/add?username=rexardiente
  // Todo: If account logout or closed the browser,
  // WS will triggered check if account has password
  // else remove account into DB
  // def addAccount(user: String) = Action.async { implicit request =>
  //   for {
  //     isExist <- accountRepo.findByUserName(user)
  //     response <- {
  //       if (isExist == None) {
  //         println(isExist)
  //         accountRepo
  //           .add(new Account(UUID.randomUUID, user, None, Instant.now().getEpochSecond))
  //           .map(x => if(x > 0) Created else InternalServerError)
  //       }
  //       else Future(InternalServerError)
  //     }
  //   } yield (response)
  // }
}