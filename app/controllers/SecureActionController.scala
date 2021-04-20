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
import models.domain.Account
import models.repo.AccountRepo
import models.service.AccountService
import auth.helpers._

@Singleton
class SecureActionController @Inject()(
                          accountRepo: AccountRepo,
                          accountService: AccountService,
                          userAction: UserAction,
                          cc: ControllerComponents,
                        ) extends AbstractController(cc) {
  // http://127.0.0.1:9000/donut/api/v1/private
  // def privateAction() = userAction.async { implicit request =>
  //   request.user
  //     .map(user => Future(Ok(user.toJson)))
  //     .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  //   // Future(Ok(""))
  // }

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