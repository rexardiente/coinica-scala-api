package controllers

import javax.inject._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

// model's import
import models.domain.game.Game
import models.repo.game.GameRepo

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      gameRepo: GameRepo,
      val controllerComponents: ControllerComponents
    ) extends BaseController {

  private def gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "description" -> optional(text),
  ))

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.index()))
  }

  def games() = Action.async { implicit request: Request[AnyContent] =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }

  def addGame() = Action.async { implicit request: Request[AnyContent] =>
    gameForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Insertion Error!!!")),
        { case (a, b) =>
          gameRepo
            .add(Game(UUID.randomUUID, a, b))
            .map(r => if(r < 0) NotFound else Created )
        }
      )

  }

  def removeGame(id: UUID) = Action.async { implicit request =>
    gameRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def updateGame(id: UUID) = Action.async { implicit request =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Update Unsuccessful!!!")),
      { case (a, b) =>
        gameRepo
          .add(Game(id, a, b))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }
  
}
