package controllers

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import models.domain.{ Game, Genre, Task }
import models.repo.{ GameRepo, GenreRepo, TaskRepo }
import models.service.{ TaskService, TransactionService }

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      taskRepo: TaskRepo,
      taskService: TaskService,
      transactionService: TransactionService,
      val controllerComponents: ControllerComponents) extends BaseController {
  /*  CUSTOM FORM VALIDATION */
  private def gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "path" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text),
  ))
 
  private def taskForm = Form(tuple(
    "gamename" -> nonEmptyText,
    "taskdate" -> optional(date("yyyy-MM-dd")),
    "description" -> optional(text),
  ))
 
  private def genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text),
  ))

  /* MAIN API */
  def index() = Action.async { implicit request =>
    Future.successful(Ok(views.html.index()))
  }

  def hello(sort: Instant, param: Instant) = Action.async { implicit request =>
    Future.successful(Ok(sort.getEpochSecond.toString + param.getEpochSecond.toString))
  }

  // def page(page: Int, pageSize: Int, totalItems: Int) = {
  //     val from = ((page - 1) * pageSize) + 1
  //     var to = from + pageSize - 1
  //     if (to > totalItems) to = totalItems
  //     var totalPages: Int = totalItems / pageSize
  //     if (totalItems % pageSize > 0) totalPages += 1
  //     (from, to, totalPages)
  // }
  /* Task API */
  def paginatedResult(limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.paginatedResult(limit, offset).map(task => Ok(Json.toJson(task)))
  }
  // def tasks(limit: Int, offset: Int) = Action.async { implicit request =>
  //   taskRepo.all(limit, offset).map(task => Ok(Json.toJson(task)))
  // }
  // def find(limit: Int, offset: Int) = Action.async { implicit request =>
  //   taskRepo.all(limit, offset).map(task => Ok(Json.toJson(task)))
  // }
  // def findTaskByID(id: UUID, limit: Int, offset: Int) = Action.async { implicit request =>
  //   taskRepo.findByID(id, limit, offset).map(task => Ok(Json.toJson(task)))
  // }
  // def findTaskByDaily(id: UUID, currentdate: Instant) = Action.async { implicit request =>
  //   taskRepo.findByDaily(id, currentdate).map(task => Ok(Json.toJson(task)))
  // }
  // def findTaskByWeekly(id: UUID, startdate: Instant, enddate: Instant) = Action.async { implicit request =>
  //   taskRepo.findByWeekly(id, startdate, enddate).map(task => Ok(Json.toJson(task)))
  // }
  // def removeTask(id: UUID) = Action.async { implicit request =>
  //    taskRepo.delete(id).map(r => if(r < 0) NotFound else Ok)
  // }

  /* GAME API */
  def games() = Action.async { implicit request =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }
  
  def addGame() = Action.async { implicit request =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (game, imgURL, path, genre, description)  =>
        for {
          isExist <- genreRepo.exist(genre)
          result  <- {
            if (isExist)
              gameRepo
                .add(Game(UUID.randomUUID, game, path, imgURL, genre, description))
                .map(r => if(r < 0) InternalServerError else Created )
            else Future.successful(InternalServerError)
          }
        } yield(result)
      }
    )
  }

  def findGameByID(id: UUID) = Action.async { implicit request =>
    gameRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def updateGame(id: UUID) = Action.async { implicit request =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (game, imgURL, path, genre, description) =>
        for {
          isExist <- genreRepo.exist(genre)
           result <- {
              if (isExist)
                gameRepo
                  .update(Game(id, game, imgURL, path, genre, description))
                  .map(r => if(r < 0) NotFound else Ok)
              else Future.successful(InternalServerError)
           }
        } yield(result)
        
      }
    )
  }

  def removeGame(id: UUID) = Action.async { implicit request =>
    gameRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  /* GENRE API */
  def genres() = Action.async { implicit request =>
    genreRepo.all().map(genre => Ok(Json.toJson(genre)))
  }

  def findGenreByID(id: UUID) = Action.async { implicit request =>
    genreRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def addGenre() = Action.async { implicit request =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description)  =>
        genreRepo
          .add(Genre(UUID.randomUUID, name, description))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }

  def updateGenre(id: UUID) = Action.async { implicit request =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        genreRepo
          .update(Genre(id, name, description))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }

  def removeGenre(id: UUID) = Action.async { implicit request =>
    genreRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def transactions(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    transactionService.paginatedResult(start, end, limit, offset).map(Ok(_))
  }
}
