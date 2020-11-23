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
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import akka.actor.ActorSystem
import akka.stream.Materializer
import models.domain.{ Game, Genre, Task, InEvent, OutEvent }
import models.repo.{ GameRepo, GenreRepo, TaskRepo }
import models.service.{ TaskService, TransactionService }
import akka.WebSocketActor

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
      implicit val system: ActorSystem, 
      mat: Materializer,
      val controllerComponents: ControllerComponents) extends BaseController {
  import models.domain.Event._
  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  /*  CUSTOM FORM VALIDATION */
  private def gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "path" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text)))
  private def taskForm = Form(tuple(
    "gamename" -> nonEmptyText,
    "taskdate" -> optional(date("yyyy-MM-dd")),
    "description" -> optional(text)))
  private def genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text)))

  def socket = WebSocket.accept[InEvent, OutEvent] { implicit request =>
    ActorFlow.actorRef { out => WebSocketActor.props(out) }
  }

  def index() = Action.async { implicit request =>
    Future.successful(Ok(views.html.index()))
  }

  def hello(sort: Instant, param: Instant) = Action.async { implicit request =>
    Future.successful(Ok(sort.getEpochSecond.toString + param.getEpochSecond.toString))
  }

  def paginatedResult(limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.paginatedResult(limit, offset).map(task => Ok(Json.toJson(task)))
  }

  def taskdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  }
    def taskdaily(start: Instant, limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByDaily(start, limit, offset).map(Ok(_))
  }
  def taskmonthly(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByMonthly(start, end, limit, offset).map(Ok(_))
  }

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
    transactionService.getTxByDateRange(start, end, limit, offset).map(Ok(_))
  }

  def transactionByTraceID(id: String) = Action.async { implicit request =>
    transactionService.getByTxTraceID(id).map(Ok(_))
  }
}
