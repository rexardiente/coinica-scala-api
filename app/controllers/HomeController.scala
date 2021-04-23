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
import play.api.libs.mailer.{ EmailValidation, MailerService }
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
      userAction: utils.auth.SecureUserAction,
      encryptKey: utils.auth.EncryptKey,
      validateEmail: EmailValidation,
      mailerService: MailerService,
      implicit val system: akka.actor.ActorSystem,
      val controllerComponents: ControllerComponents) extends BaseController {
  implicit val messageFlowTransformer = utils.MessageTransformer.jsonMessageFlowTransformer[Event, Event]

  /*  CUSTOM FORM VALIDATION */
  private val challengeForm = Form(tuple(
    "name" -> uuid,
    "description" -> nonEmptyText,
    "start_at" -> longNumber,
    "expire_at" -> optional(longNumber)))
  private val rankingForm = Form(tuple(
    "name" -> nonEmptyText,
    "bets" -> number,
    "profit" -> number,
    "multiplieramount" -> number,
    "rankingcreated" -> longNumber))
  private val gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "path" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text)))
  private val taskForm = Form(tuple(
    "gameid" -> uuid,
    "info" -> nonEmptyText,
    "isValid" -> boolean,
    "datecreated" -> longNumber))
  private val genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text)))

  private val signForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText))
  // referred_by = user code..
  private val signUpForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText,
    "referred_by" -> optional(text)))
  private val emailForm = Form(single("email" -> email.verifying( play.api.data.validation.Constraints.emailAddress )))

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

  def signUp = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password, code)  =>
        // validate username and referral code..
        for {
          isAccountAlreadyExists <- userAccountService.getAccountByName(username)
          hasReferralCode <- userAccountService.getAccountByCode(code.getOrElse(null))
          processed <- {
            if (isAccountAlreadyExists == None) {
              if (hasReferralCode.map(_.referralCode) == code) {
                // encrypt account password into SHA256 algorithm...
                val user: UserAccount = UserAccount(username, encryptKey.toSHA256(password))
                // generate User Account and VIP Account
                for {
                  addAccount <- userAccountService.newUserAcc(user)
                  addVip <- userAccountService.newVIPAcc(VIPUser(user.id, VIP.BRONZE, VIP.BRONZE, 0, 0, 0, user.createdAt))
                  // apply code if has value
                  _ <- Future.successful {
                    Thread.sleep(500) // add small delay
                    if (hasReferralCode.map(_.referralCode) != None) {
                      referralHistoryService.applyReferralCode(user.id, code.getOrElse(null))
                    }
                  }
                } yield (Created)
              }
              else Future(InternalServerError)
            }
            else Future(Conflict)
          }
        } yield (processed)
      })
  }

  def signOut() = Action.async { implicit request =>
    signForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        userAccountService
          .getAccountByUserNamePassword(username, encryptKey.toSHA256(password))
          .map {
            case Some(account) =>
              userAccountService
                .updateUserAccount(account.copy(token=None, tokenLimit=None))
                .map(x => if (x > 0) Accepted else InternalServerError)
            case _ => Future(Unauthorized(views.html.defaultpages.unauthorized()))
          }.flatten
      })
  }
  // TODO: store password in hash256 format...
  def signIn(verify: Option[Long]) = Action.async { implicit request =>
    signForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        try {
          for {
            // check if sigin is from verifications no need Encryption
            // else Encryptkey for security validation
            isAccountExists <- {
              if (verify != None) userAccountService.getAccountByUserNamePassword(username, password)
              else userAccountService.getAccountByUserNamePassword(username, encryptKey.toSHA256(password))
            }
            processed <- {
              // check if has existing token (UPDATE) else insert new and return to user..
              if (isAccountExists != None) {
                val account: UserAccount = isAccountExists.get
                val tempAccount: UserAccount = userAction.generateToken(account)
                val currentTime: Long = Instant.now.getEpochSecond
                // if verify make sure that existing token wont be overrided on this request..
                if (verify != None) {
                  // if expiration is greater than current time then its valid
                  if (verify.getOrElse(0L) >= currentTime) {
                    if (account.tokenLimit.map(_ <= currentTime).getOrElse(true)) {
                      userAccountService
                        .updateUserAccount(tempAccount)
                        .map(x => if (x > 0) Ok(Json.obj("token" -> tempAccount.token)) else InternalServerError)
                    }
                    else Future(Ok(Json.obj("token" -> account.token)))
                  }
                  else Future(Forbidden)
                } else {
                  if (account.tokenLimit.map(_ <= currentTime).getOrElse(true)) {
                    userAccountService
                      .updateUserAccount(tempAccount.copy(lastSignIn = Instant.now))
                      .map(x => if (x > 0) Ok(Json.obj("token" -> tempAccount.token)) else InternalServerError)
                  }
                  else Future(Forbidden)
                }
              }
              else Future(Unauthorized(views.html.defaultpages.unauthorized()))
            }
          } yield (processed)
        }
        catch { case _: Throwable => Future(InternalServerError) }
      })
  }
  def emailVerification(code: String) = Action.async { implicit request =>
    try {
      validateEmail.emailFromCode(code) map {
        case (u, p, e, x) => Ok(views.html.emailVerificationTemplate(u, p)(e, x))
        case _ => NotFound
    }}
    catch { case e: Throwable => Future(NotFound) }
  }
  // Check if email is valid email and if email exist then send confirmation link..
  def resetPassword() = Action.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (email)  =>
        for {
          account <- userAccountService.getAccountByEmailAddress(email)
          processed <- {
            if (account != None) // send email confirmation link
              mailerService.sendResetPasswordEmail(account.get, email).map(_ => Created)
            else Future(NotFound)
          }
        } yield (processed)
      })
  }
  def resetPasswordVerification(code: String) = Action.async { implicit request =>
    try {
      validateEmail.passwordFromCode(code) map {
        case (u, p, e) => Ok(views.html.resetPasswordTemplate(u, p, e))
        case _ => NotFound
    }}
    catch { case e: Throwable => Future(NotFound) }
  }
  def addChallenge = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, startAt, expiredAt)  =>
        challengeRepo
          .add(Challenge(name, description, startAt, expiredAt.getOrElse(startAt + 86400)))
          .map(r => if(r > 0) Created else InternalServerError)
      })
  }
  // TODO: Need enhancements
  def updateChallenge(id: UUID) = Action.async { implicit req =>
    challengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description, startAt, expiredAt)  =>
        challengeRepo
          .update(Challenge(id, name, description, startAt, expiredAt.getOrElse(Instant.now.getEpochSecond)))
          .map(r => if(r > 0) Ok else NotFound)
      })
  }

  def removeChallenge(id: UUID) = Action.async { implicit req =>
    challengeRepo
      .delete(id)
      .map(r => if(r > 0) Ok else NotFound)
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
  //         .map(r => if(r > 0) Created else InternalServerError)
  //     })
  // }
  // def updateTask(id: UUID) = Action.async { implicit req =>
  //   taskForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (gameID, info, isValid, datecreated) =>
  //       taskRepo
  //         .update(Task(id, gameID, info, isValid, datecreated))
  //         .map(r => if(r > 0) Ok else NotFound)
  //     })
  // }
  // def removeTask(id: UUID) = Action.async { implicit req =>
  //   taskRepo
  //     .delete(id)
  //     .map(r => if(r > 0) Ok else NotFound)
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
  //         .map(r => if(r > 0) Created else InternalServerError)
  //     })
  // }
  // def updateRanking(id: UUID) = Action.async { implicit req =>
  //   rankingForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (name, bets,profit,multiplieramount,rankingcreated) =>
  //       rankingRepo
  //         .update(Ranking(id, name, bets,profit,multiplieramount,rankingcreated))
  //         .map(r => if(r > 0) Ok else NotFound)
  //     })
  // }
  // def removeRanking(id: UUID) = Action.async { implicit req =>
  //   rankingRepo
  //     .delete(id)
  //     .map(r => if(r > 0) Ok else NotFound)
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
          processed  <- {
            if (isExist)
              gameRepo
                .add(Game(UUID.randomUUID, game, path, imgURL, genre, description))
                .map(r => if(r > 0) Created else InternalServerError)
            else Future(NotFound)
          }
        } yield(processed)
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
           processed <- {
              if (isExist)
                gameRepo
                  .update(Game(id, game, imgURL, path, genre, description))
                  .map(r => if(r > 0) Ok else InternalServerError)
              else Future(NotFound)
           }
        } yield(processed)

      })
  }

  def removeGame(id: UUID) = Action.async { implicit req =>
    gameRepo
      .delete(id)
      .map(r => if(r > 0) Ok else NotFound)
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
          .map(r => if(r > 0) Created else InternalServerError)
      })
  }

  def updateGenre(id: UUID) = Action.async { implicit req =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        genreRepo
          .update(Genre(id, name, description))
          .map(r => if(r > 0) Ok else InternalServerError)
      })
  }

  def removeGenre(id: UUID) = Action.async { implicit req =>
    genreRepo
      .delete(id)
      .map(r => if(r > 0) Ok else NotFound)
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