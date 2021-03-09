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
import play.api.libs.json._
import models.domain._
import models.repo._
import models.repo.eosio._
import models.service._
import utils.lib.EOSIOSupport
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
      newsRepo: NewsRepo,
      taskService: TaskService,
      rankingService: RankingService,
      referralService:  ReferralService,
      transactionService: TransactionService,
      challengeService: ChallengeService,
      gQCharacterDataRepo: GQCharacterDataRepo,
      gQCharacterGameHistoryRepo: GQCharacterGameHistoryRepo,
      overAllGameHistoryRepo: OverAllGameHistoryRepo,
      gqGameService: GQGameService,
      eosio: EOSIOSupport,
      gqSmartContractAPI: GQSmartContractAPI,
      mat: akka.stream.Materializer,
      implicit val system: akka.actor.ActorSystem,
      val controllerComponents: ControllerComponents) extends BaseController {
  implicit val messageFlowTransformer = utils.MessageTransformer.jsonMessageFlowTransformer[Event, Event]

  /*  CUSTOM FORM VALIDATION */
  private def referralForm = Form(tuple(
    "referralname" -> nonEmptyText,
    "referrallink" -> nonEmptyText,
    "rate" -> number,
    "feeamount" -> number,
    "referralcreated" -> longNumber))
  private def challengeForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> nonEmptyText,
    "is_avaiable" -> boolean,
    "start_at" -> longNumber,
    "expire_at" -> optional(longNumber)))
  private def rankingForm = Form(tuple(
    "name" -> nonEmptyText,
    "bets" -> number,
    "profit" -> number,
    "multiplieramount" -> number,
    "rankingcreated" -> longNumber))
  private def gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "path" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text)))
  private def taskForm = Form(tuple(
    "gameid" -> uuid,
    "info" -> nonEmptyText,
    "isValid" -> boolean,
    "datecreated" -> longNumber))
  private def genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text)))

  def socket = WebSocket.accept[Event, Event] { implicit req =>
    play.api.libs.streams.ActorFlow.actorRef { out =>
      WebSocketActor.props(out, gQCharacterDataRepo, gQCharacterGameHistoryRepo, overAllGameHistoryRepo, eosio, gqSmartContractAPI)
    }
  }

  def index() = Action.async { implicit req =>
    Future.successful(Ok(views.html.index()))
  }

  def addReferral = Action.async { implicit req =>
    referralForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (referralname, referrallink,rate,feeamount,referralcreated)  =>
        referralRepo
          .add(Referral(UUID.randomUUID, referralname, referrallink,rate,feeamount,referralcreated))
          .map(r => if(r < 0) InternalServerError else Created )
      })
  }

  def updateReferral(id: UUID) = Action.async { implicit req =>
    referralForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (referralname, referrallink,rate,feeamount,referralcreated) =>
        referralRepo
          .update(Referral(id, referralname, referrallink,rate,feeamount,referralcreated))
          .map(r => if(r < 0) NotFound else Ok)
      })
  }

  def removeReferral(id: UUID) = Action.async { implicit req =>
    referralRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def addChallenge = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, isAvailable, startAt, expireAt)  =>
        try {
          challengeRepo
            .add(Challenge(name,
                          description,
                          Instant.ofEpochSecond(startAt),
                          expireAt.map(Instant.ofEpochSecond).getOrElse(Instant.ofEpochSecond(startAt + 86400)),
                          isAvailable))
            .map(r => if(r < 0) InternalServerError else Created )
        } catch {
          case _: Throwable => Future(InternalServerError)
        }
      })
  }

  // TODO: Need enhancements
  def updateChallenge(id: UUID) = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, isAvailable, startAt, expireAt)  =>
        try {
          challengeRepo
            .update(Challenge(id,
                              name,
                              description,
                              Instant.ofEpochSecond(startAt),
                              expireAt.map(Instant.ofEpochSecond).getOrElse(Instant.ofEpochSecond(startAt + 86400)),
                              isAvailable,
                              Instant.now))
            .map(r => if(r < 0) NotFound else Ok)
        } catch {
          case _: Throwable => Future(InternalServerError)
        }
      })
  }

  def removeChallenge(id: UUID) = Action.async { implicit req =>
    challengeRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def getChallengeByDate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    challengeService.getChallengeByDate(start, end, limit, offset).map(Ok(_))
  }

  def addTask = Action.async { implicit req =>
    taskForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (gameID, info, isValid, datecreated)  =>
        taskRepo
          .add(Task(UUID.randomUUID, gameID, info, isValid, datecreated))
          .map(r => if(r < 0) InternalServerError else Created )
      })
  }

  def updateTask(id: UUID) = Action.async { implicit req =>
    taskForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (gameID, info, isValid, datecreated) =>
        taskRepo
          .update(Task(id, gameID, info, isValid, datecreated))
          .map(r => if(r < 0) NotFound else Ok)
      })
  }

  def removeTask(id: UUID) = Action.async { implicit req =>
    taskRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def taskdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  }

  def taskdaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  }

  def referraldate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }

  def referraldaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
       referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }

  def addRanking = Action.async { implicit req =>
    rankingForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,multiplieramount,rankingcreated)  =>
        rankingRepo
          .add(Ranking(UUID.randomUUID, name, bets,profit,multiplieramount,rankingcreated))
          .map(r => if(r < 0) InternalServerError else Created )
      })
  }

  def updateRanking(id: UUID) = Action.async { implicit req =>
    rankingForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, bets,profit,multiplieramount,rankingcreated) =>
        rankingRepo
          .update(Ranking(id, name, bets,profit,multiplieramount,rankingcreated))
          .map(r => if(r < 0) NotFound else Ok)
      })
  }

  def removeRanking(id: UUID) = Action.async { implicit req =>
    rankingRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def rankingdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    rankingService.getRankingByDate(start, end, limit, offset).map(Ok(_))
  }

  def rankingdaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
      rankingService.getRankingByDate(start, end, limit, offset).map(Ok(_))
  }

  def taskmonthly(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    taskService.getTaskByMonthly(start, end, limit, offset).map(Ok(_))
  }

  def referralmonthly(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    referralService.getReferralByDate(start, end, limit, offset).map(Ok(_))
  }

  def games() = Action.async { implicit req =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }

  def addGame() = Action.async { implicit req =>
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
      })
  }

  def findGameByID(id: UUID) = Action.async { implicit req =>
    gameRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def updateGame(id: UUID) = Action.async { implicit req =>
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

      })
  }

  def removeGame(id: UUID) = Action.async { implicit req =>
    gameRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  /* GENRE API */
  def genres() = Action.async { implicit req =>
    genreRepo.all().map(genre => Ok(Json.toJson(genre)))
  }

  def findGenreByID(id: UUID) = Action.async { implicit req =>
    genreRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def addGenre() = Action.async { implicit req =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description)  =>
        genreRepo
          .add(Genre(UUID.randomUUID, name, description))
          .map(r => if(r < 0) InternalServerError else Created )
      })
  }

  def updateGenre(id: UUID) = Action.async { implicit req =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        genreRepo
          .update(Genre(id, name, description))
          .map(r => if(r < 0) NotFound else Ok)
      })
  }

  def removeGenre(id: UUID) = Action.async { implicit req =>
    genreRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  def transactions(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    transactionService.getTxByDateRange(start, end, limit, offset).map(Ok(_))
  }

  def transactionByTraceID(id: String) = Action.async { implicit req =>
    transactionService.getByTxTraceID(id).map(Ok(_))
  }

  def getAllCharacters() = Action.async { implicit req =>
    gQCharacterDataRepo.all().map(x => Ok(Json.toJson(x)))
  }

  def getAllCharactersByUser[T <: String](user: T) = Action.async { implicit req =>
    gqGameService.getAllCharactersDataAndHistoryLogsByUser(user).map(Ok(_))
  }

  def getCharactersByUser[T <: String](user: T) = Action.async { implicit req =>
    gqGameService.getAliveCharacters(user).map(Ok(_))
  }

  def getCharacterByID(id: String) = Action.async { implicit req =>
    gqGameService.getCharacterDataByID(id).map(Ok(_))
  }

  def getCharacterByUserAndID[T <: String](user: T, id: T) = Action.async { implicit req =>
    gqGameService.getCharacterByUserAndID(user, id).map(Ok(_))
  }

  def getCharacterHistoryByUser(user: String) = Action.async { implicit req =>
    gqGameService.getAllEliminatedCharacters(user).map(Ok(_))
  }

  def getCharacterHistoryByUserAndID[T <: String](user: T, id: T) = Action.async { implicit req =>
    gqGameService.getCharacterHistoryByUserAndID(user, id).map(Ok(_))
  }

  def getAllGQGameHistory() = Action.async { implicit req =>
    gQCharacterGameHistoryRepo.all().map(x => Ok(Json.toJson(x)))
  }

  def getGQGameHistoryByUser(user: String) = Action.async { implicit req =>
    gQCharacterGameHistoryRepo.getByUser(user).map(x => Ok(Json.toJson(x)))
  }

  def getGQGameHistoryByUserAndCharacterID[T <: String](user: T, id: T) = Action.async { implicit req =>
    gQCharacterGameHistoryRepo.getByUsernameAndCharacterID(id, user).map(x => Ok(Json.toJson(x)))
  }
  def getGQGameHistoryByGameID(id: String) = Action.async { implicit req =>
    gQCharacterGameHistoryRepo.filteredByID(id).map(x => Ok(Json.toJson(x)))
  }

  def highEarnCharactersAllTime() = Action.async { implicit req =>
    gqGameService.highEarnCharactersAllTime().map(x => Ok(Json.toJson(x)))
  }

  def highEarnCharactersDaily() = Action.async { implicit req =>
    gqGameService.highEarnCharactersDaily().map(x => Ok(Json.toJson(x)))
  }

  def highEarnCharactersWeekly() = Action.async { implicit req =>
    gqGameService.highEarnCharactersWeekly().map(x => Ok(Json.toJson(x)))
  }

  def winStreakPerDay() = Action.async { implicit req =>
    gqGameService.winStreakPerDay().map(x => Ok(Json.toJson(x)))
  }

  def winStreakPerWeekly() = Action.async { implicit req =>
    gqGameService.winStreakPerWeekly().map(x => Ok(Json.toJson(x)))
  }

  def winStreakLifeTime() = Action.async { implicit req =>
    gqGameService.winStreakLifeTime().map(x => Ok(Json.toJson(x)))
  }

  def news() = Action.async { implicit req =>
    newsRepo.all().map(x => Ok(Json.toJson(x)))
  }
}