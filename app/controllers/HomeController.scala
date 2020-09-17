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
import models.domain.game.Genre
import models.repo.game.GameRepo
import models.repo.game.GenreRepo

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      val controllerComponents: ControllerComponents
    ) extends BaseController {
  
  /*  CUSTOM FORM VALIDATION */
  private def gameForm = Form(tuple(
    "name" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "path" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text),
  ))

  private def genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text),
  ))

  /* MAIN API */
  def index() = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.index()))
  }

  /* GAME API */
  def games() = Action.async { implicit request: Request[AnyContent] =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }

  def addGame() = Action.async { implicit request: Request[AnyContent] =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, imgURL, path, genre, description)  =>
        gameRepo
          .add(Game(UUID.randomUUID, name, imgURL, path, genre, description))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }

  def findGameByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def updateGame(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, imgURL, path, genre, description) =>
        for {
          isExist <- gameRepo.exist(id)
          result <- {
            if (isExist)
              gameRepo
                .update(Game(id, name, imgURL, path, genre, description))
                .map(r => if(r < 0) NotFound else Ok)
            else Future.successful(InternalServerError)
          }
        } yield(result)
        
      }
    )
  }

  def removeGame(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  /* GENRE API */
  def genres() = Action.async { implicit request: Request[AnyContent] =>
    genreRepo.all().map(genre => Ok(Json.toJson(genre)))
  }

  def findGenreByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def addGenre() = Action.async { implicit request: Request[AnyContent] =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description)  =>
        genreRepo
          .add(Genre(UUID.randomUUID, name, description))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }

  def updateGenre(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        for {
          isExist <- genreRepo.exist(id)
          result <- {
            if (isExist)
              genreRepo
                .update(Genre(id, name, description))
                .map(r => if(r < 0) NotFound else Ok)
            else Future.successful(InternalServerError)
          }
        } yield(result)
      }
    )
  }

  def removeGenre(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
  
}
