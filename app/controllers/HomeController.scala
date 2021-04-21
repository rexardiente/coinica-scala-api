package controllers

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.Configuration
import play.api.http.HttpErrorHandler
import models.domain._
import models.repo._
import models.repo.eosio._
import models.service._
import models.domain.enum._
import akka.WebSocketActor
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      // userAccRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      userAccountService: UserAccountService,
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      challengeRepo: ChallengeRepo,
      newsRepo: NewsRepo,
      taskService: TaskService,
      rankingService: RankingService,
      referralHistoryService:  ReferralHistoryService,
      eosNetTransaction: EOSNetTransactionService,
      challengeService: ChallengeService,
      gQCharacterDataRepo: GQCharacterDataRepo,
      gQCharacterGameHistoryRepo: GQCharacterGameHistoryRepo,
      overAllGameHistoryRepo: OverAllGameHistoryRepo,
      overAllHistoryService: OverAllHistoryService,
      gqGameService: GQGameService,
      eosioHTTPSupport: akka.EOSIOHTTPSupport,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
      mat: akka.stream.Materializer,
      assets: Assets,
      errorHandler: HttpErrorHandler,
      userAction: auth.helpers.SecureUserAction,
      implicit val system: akka.actor.ActorSystem,
      val controllerComponents: ControllerComponents) extends BaseController {
  implicit val messageFlowTransformer = utils.MessageTransformer.jsonMessageFlowTransformer[Event, Event]

  /*  CUSTOM FORM VALIDATION */
  private def challengeForm = Form(tuple(
    "name" -> uuid,
    "description" -> nonEmptyText,
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

  private def signInForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText))
  // referred_by = user code..
  private def signUpForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText,
    "referred_by" -> optional(text)))
  // http://127.0.0.1:9000/donut/api/v1/signin?username=rexardiente&password=password
  private def isValidLogin(username: String, password: String): Future[Boolean] = Future(true)
    // accountRepo.isExist(username, password)

  def signUp = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password, code)  =>
        // validate username and referral code..
        // after validation encode password into Base256
        for {
          account <- userAccountService.getAccountByName(username)
          hasCode <- userAccountService.getAccountByCode(code.getOrElse(null))
          result <- {
            if (account == None) {
              if (hasCode.map(_.referral_code) == code) {
                val user: UserAccount = UserAccount(username, password)
                // generate User Account and VIP Account
                for {
                  addAccount <- userAccountService.newUserAcc(user)
                  addVip <- userAccountService.newVIPAcc(VIPUser(user.id, VIP.BRONZE, VIP.BRONZE, 0, 0, 0, user.created_at))
                  // apply code if has value
                  _ <- Future.successful {
                    Thread.sleep(500) // add small delay
                    if (hasCode.map(_.referral_code) != None) {
                      referralHistoryService.applyReferralCode(user.id, code.getOrElse(null))
                    }
                  }
                } yield (Created)
              }
              else Future(InternalServerError)
            }
            else Future(InternalServerError)
          }
        } yield (result)
      })
  }
  // TODO: store password in hash256 format...
  def signIn = Action.async { implicit request =>
    signInForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        try {
          for {
            isAccountExists <- userAccountService.getAccountByUserNamePassword(username, password)
            result <- {
              // check if has existing token (UPDATE) else insert new and return to user..
              if (isAccountExists != None) {
                val account: UserAccount = isAccountExists.get
                // check if token is not yet expired..
                if (account.tokenLimit.map(_ <= Instant.now.getEpochSecond).getOrElse(true)) {
                  // new generated token
                  val newUserToken: UserAccount = userAction.generateToken(account)
                  userAccountService
                    .updateUserAccount(newUserToken.copy(lastSignIn = Instant.now))
                    .map { x =>
                      if (x > 0) Ok(Json.obj("token" -> newUserToken.token, "limit" -> newUserToken.tokenLimit))
                      else InternalServerError
                    }
                }
                else Future(Conflict(Json.obj("exists" -> account.token, "limit" -> account.tokenLimit)))
                // else Future(Conflict(Json.obj("error" -> "already signed")))
              }
              else Future(InternalServerError)
            }
          } yield (result)
        }
        catch { case _: Throwable => Future(InternalServerError) }
      })
  }

  def socket = WebSocket.accept[Event, Event] { implicit req =>
    play.api.libs.streams.ActorFlow.actorRef { out =>
      WebSocketActor.props(out,
                          userAccountService,
                          gQCharacterDataRepo,
                          gQCharacterGameHistoryRepo,
                          overAllGameHistoryRepo,
                          vipUserRepo,
                          eosioHTTPSupport,
                          dynamicBroadcast,
                          dynamicProcessor)
    }
  }

  def index(): Action[AnyContent] = assets.at("index.html")
  def assetOrDefault(resource: String): Action[AnyContent] =
    try {
      if (resource.contains(".")) assets.at(resource) else index
    } catch {
      case e: Throwable => Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
    }

  def addChallenge = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, startAt, expiredAt)  =>
        try {
          challengeRepo
            .add(Challenge(name,
                          description,
                          startAt,
                          expiredAt.getOrElse(startAt + 86400)))
            .map(r => if(r < 1) InternalServerError else Created )
        } catch {
          case _: Throwable => Future(InternalServerError)
        }
      })
  }

  // TODO: Need enhancements
  def updateChallenge(id: UUID) = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, startAt, expiredAt)  =>
        try {
          challengeRepo
            .update(Challenge(id,
                              name,
                              description,
                              startAt,
                              expiredAt.getOrElse(Instant.now.getEpochSecond)))
            .map(r => if(r < 1) NotFound else Ok)
        } catch {
          case _: Throwable => Future(InternalServerError)
        }
      })
  }

  def removeChallenge(id: UUID) = Action.async { implicit req =>
    challengeRepo
      .delete(id)
      .map(r => if(r < 1) NotFound else Ok)
  }

  def getChallenge(date: Option[Instant]) = Action.async { implicit req =>
    challengeService.getChallenge(date).map(_.map(x => Ok(x.toJson)).getOrElse(Ok(JsNull)))
  }

  def getDailyRanksChallenge() = Action.async { implicit req =>
    challengeService.getDailyRanksChallenge.map(x => Ok(Json.toJson(x)))
  }

  // def getWeeklyTaskUpdates(user: String, gameID: UUID)
  // def addTask = Action.async { implicit req =>
  //   taskForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (gameID, info, isValid, datecreated)  =>
  //       taskRepo
  //         .add(Task(UUID.randomUUID, gameID, info, isValid, datecreated))
  //         .map(r => if(r < 1) InternalServerError else Created )
  //     })
  // }
  // def updateTask(id: UUID) = Action.async { implicit req =>
  //   taskForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (gameID, info, isValid, datecreated) =>
  //       taskRepo
  //         .update(Task(id, gameID, info, isValid, datecreated))
  //         .map(r => if(r < 1) NotFound else Ok)
  //     })
  // }
  // def removeTask(id: UUID) = Action.async { implicit req =>
  //   taskRepo
  //     .delete(id)
  //     .map(r => if(r < 1) NotFound else Ok)
  // }
  // def taskdate(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
  //   taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  // }
  // def taskdaily(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
  //   taskService.getTaskByDate(start, end, limit, offset).map(Ok(_))
  // }
  // def addRanking = Action.async { implicit req =>
  //   rankingForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (name, bets,profit,multiplieramount,rankingcreated)  =>
  //       rankingRepo
  //         .add(Ranking(UUID.randomUUID, name, bets,profit,multiplieramount,rankingcreated))
  //         .map(r => if(r < 1) InternalServerError else Created )
  //     })
  // }
  // def updateRanking(id: UUID) = Action.async { implicit req =>
  //   rankingForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (name, bets,profit,multiplieramount,rankingcreated) =>
  //       rankingRepo
  //         .update(Ranking(id, name, bets,profit,multiplieramount,rankingcreated))
  //         .map(r => if(r < 1) NotFound else Ok)
  //     })
  // }
  // def removeRanking(id: UUID) = Action.async { implicit req =>
  //   rankingRepo
  //     .delete(id)
  //     .map(r => if(r < 1) NotFound else Ok)
  // }

  // def getRankingByDate(date: Option[Instant]) = Action.async { implicit req =>
  //   rankingService.getRankingByDate(date).map(_.map(x => Ok(x.toJson)).getOrElse(Ok(JsNull)))
  // }

  def getRankingDaily() = Action.async { implicit req =>
    rankingService.getRankingDaily().map(x => Ok(Json.toJson(x)))
  }
  def getRankingHistory() = Action.async { implicit req =>
    rankingService.getRankingHistory().map(x => Ok(Json.toJson(x)))
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
                .map(r => if(r < 1) InternalServerError else Created )
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
                  .map(r => if(r < 1) InternalServerError else Ok)
              else Future.successful(InternalServerError)
           }
        } yield(result)

      })
  }

  def removeGame(id: UUID) = Action.async { implicit req =>
    gameRepo
      .delete(id)
      .map(r => if(r < 1) NotFound else Ok)
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
          .map(r => if(r < 1) InternalServerError else Created )
      })
  }

  def updateGenre(id: UUID) = Action.async { implicit req =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        genreRepo
          .update(Genre(id, name, description))
          .map(r => if(r < 1) InternalServerError else Ok)
      })
  }

  def removeGenre(id: UUID) = Action.async { implicit req =>
    genreRepo
      .delete(id)
      .map(r => if(r < 1) NotFound else Ok)
  }

  def transactions(start: Instant, end: Option[Instant], limit: Int, offset: Int) = Action.async { implicit req =>
    eosNetTransaction.getTxByDateRange(start, end, limit, offset).map(Ok(_))
  }

  def transactionByTraceID(id: String) = Action.async { implicit req =>
    eosNetTransaction.getByTxTraceID(id).map(Ok(_))
  }

  def news() = Action.async { implicit req =>
    newsRepo.all().map(x => Ok(Json.toJson(x)))
  }
}