package controllers

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import models.domain.Login
import models.repo.LoginRepo

@Singleton
class AdminController @Inject()(loginRepo: LoginRepo, val controllerComponents: ControllerComponents) extends BaseController {

  private def loginForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText))

  def addLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        loginRepo
          .add(Login( username, password))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }

  def updateLogin(username: String) = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password) =>
        loginRepo
          .update(Login( username, password))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }
 
  def removeLogin(username: String) = Action.async { implicit request =>
    loginRepo
      .delete(username)
      .map(r => if(r < 0) NotFound else Ok)
  }
  
}