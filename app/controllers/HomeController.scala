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
// import play.api.data.format.Formats._
import play.api.libs.json._
import models.domain.{ Login, Game, Genre, Task, Ranking, Challenge, Referral, InEvent, OutEvent }
import models.repo.{ LoginRepo, GameRepo, GenreRepo, TaskRepo, ReferralRepo, RankingRepo, TransactionRepo, ChallengeRepo }
import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo, GQCharacterDataHistoryRepo }
import models.service.{ TaskService, ReferralService, RankingService, TransactionService, ChallengeService }
import akka.WebSocketActor
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()( 
      loginRepo: LoginRepo,
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      taskRepo: TaskRepo,
      rankingRepo: RankingRepo,
      challengeRepo: ChallengeRepo,
      referralRepo: ReferralRepo,
      taskService: TaskService,
      rankingService: RankingService,
      referralService:  ReferralService,
      transactionService: TransactionService,
      challengeService: ChallengeService,
      gQCharacterDataRepo: GQCharacterDataRepo,
      gQCharacterDataHistoryRepo: GQCharacterDataHistoryRepo,
      gQCharacterGameHistoryRepo: GQCharacterGameHistoryRepo,
      implicit val system: akka.actor.ActorSystem, 
      mat: akka.stream.Materializer,
      val controllerComponents: ControllerComponents) extends BaseController {
  import models.domain.Event._
  implicit val messageFlowTransformer = utils.MessageTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  /*  CUSTOM FORM VALIDATION */

  private def challengeForm = Form(tuple(
    "name" -> nonEmptyText,
    "bets" -> number,
    "profit" -> number,
    "ratio" -> number,
    "vippoints" -> number,
    "challengecreated" -> number))

  private def rankingForm = Form(tuple(
    "name" -> nonEmptyText,
    "bets" -> number,
    "profit" -> number,
    "multiplieramount" -> number,
    "rankingcreated" -> number))
  
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
    play.api.libs.streams.ActorFlow.actorRef { out => WebSocketActor.props(out) }
  }

  def index() = Action.async { implicit request =>
    Future.successful(Ok(views.html.index()))
  }

  def paginatedResult(limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.paginatedResult(limit, offset).map(task => Ok(Json.toJson(task)))
  }
  def addChallenge = Action.async { implicit request =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,ratio,vippoints,challengecreated)  =>
        challengeRepo
          .add(Challenge(UUID.randomUUID, name, bets,profit,ratio,vippoints,challengecreated))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }
  def updateChallenge(id: UUID) = Action.async { implicit request =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,ratio,vippoints,challengecreated) =>
        challengeRepo
          .update(Challenge(id, name, bets,profit,ratio,vippoints,challengecreated))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }
  def removeChallenge(id: UUID) = Action.async { implicit request =>
    challengeRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
   def challengedate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    challengeService.getChallengeByDate(start, end, limit, offset).map(Ok(_))
  }
   def challengedaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    challengeService.getChallengeByDate(start, end, limit, offset).map(Ok(_))
  }

  def taskdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  }
  def taskdaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  }
  def referraldate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }
    def referraldaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
       referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }
  def addRanking = Action.async { implicit request =>
    rankingForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,multiplieramount,rankingcreated)  =>
        rankingRepo
          .add(Ranking(UUID.randomUUID, name, bets,profit,multiplieramount,rankingcreated))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }
  def updateRanking(id: UUID) = Action.async { implicit request =>
    rankingForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,multiplieramount,rankingcreated) =>
        rankingRepo
          .update(Ranking(id, name, bets,profit,multiplieramount,rankingcreated))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }
  def removeRanking(id: UUID) = Action.async { implicit request =>
    rankingRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
   def rankingdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    rankingService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }
    def rankingdaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
      rankingService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }
  def taskmonthly(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    taskService.getTaskByMonthly(start, end, limit, offset).map(Ok(_))
  }
  def referralmonthly(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit request =>
    referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
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

  def getAllCharacters() = Action.async { implicit request =>
    gQCharacterDataRepo.all().map(x => Ok(Json.toJson(x)))
  }

  def getCharactersByUser[T <: String](user: T) = Action.async { implicit request => 
    gQCharacterDataRepo.findByUser(user).map(x => Ok(Json.toJson(x)))
  }

  def getCharacterByUserAndID[T <: String](user: T, id: T) = Action.async { implicit request =>
    gQCharacterDataRepo.findByUserAndID(user, id).map(x => Ok(Json.toJson(x)))
  }

  def getAllGQGameHistory() = Action.async { implicit request =>
    gQCharacterGameHistoryRepo.all().map(x => Ok(Json.toJson(x)))
  }

  def getGQGameHistoryByUser(user: String) = Action.async { implicit request =>
    gQCharacterGameHistoryRepo.getByUser(user).map(x => Ok(Json.toJson(x)))
  }

  def getGQGameHistoryByUserAndID[T <: String](user: T, id: T) = Action.async { implicit request =>
    gQCharacterGameHistoryRepo.find(id, user).map(x => Ok(Json.toJson(x)))
  }
  def getGQGameHistoryByGameID(id: UUID) = Action.async { implicit request =>
    gQCharacterGameHistoryRepo.findByGameID(id).map(x => Ok(Json.toJson(x)))
  }

  def getCharacterHistoryByUser(user: String) = Action.async { implicit request =>
    gQCharacterDataHistoryRepo.findByUser(user).map(x => Ok(Json.toJson(x)))
  }

  def getCharacterHistoryByID(id: String) = Action.async { implicit request =>
    gQCharacterDataHistoryRepo.findByID(id).map(x => Ok(Json.toJson(x)))
  }
}
