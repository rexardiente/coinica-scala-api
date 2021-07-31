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
import utils.Config
import models.domain.wallet.support.Coin
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      // userAccRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      accountService: UserAccountService,
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      challengeRepo: ChallengeRepo,
      newsRepo: NewsRepo,
      taskService: TaskService,
      rankingService: RankingService,
      referralHistoryService:  ReferralHistoryService,
      eosNetTransaction: EOSNetTransactionService,
      challengeService: ChallengeService,
      overAllGameHistoryRepo: OverAllGameHistoryRepo,
      // overAllHistoryService: OverAllHistoryService,
      ghostQuestEOSIO: utils.lib.GhostQuestEOSIO,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
      mat: akka.stream.Materializer,
      assets: Assets,
      errorHandler: HttpErrorHandler,
      userAction: utils.auth.SecureUserAction,
      encryptKey: utils.auth.EncryptKey,
      validateEmail: EmailValidation,
      mailerService: MailerService,
      multiCurrencySupport: utils.lib.MultiCurrencyHTTPSupport,
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
  private val emailForm = Form(single(
    "email" -> email.verifying(play.api.data.validation.Constraints.emailAddress)))
  private val resetPasswordForm = Form(tuple(
    "username" -> nonEmptyText,
    "new_password" -> nonEmptyText,
    "confirm_password" -> nonEmptyText
  ).verifying("Password and Confirm password does not match", info => info._2 == info._3))


  def socket = WebSocket.accept[Event, Event] { implicit req =>
    play.api.libs.streams.ActorFlow.actorRef { out =>
      WebSocketActor.props(out,
                          accountService,
                          overAllGameHistoryRepo,
                          vipUserRepo,
                          ghostQuestEOSIO,
                          dynamicBroadcast,
                          dynamicProcessor)
    }
  }

  def redirect(to: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(to))
  }

  def index() = Action.async { implicit request =>
    Future.successful(Ok(JsString("ok")))
  }
  def submitNewPassword() = Action.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password, confirm)  =>
        for {
          // get account by username
          accountOpt <- accountService.getAccountByName(username)
          // update account password if exists
          processed <- {
            accountOpt
              .map { acc =>
                for {
                  // remove reset email token
                  removed <- accountService.removePasswordTokenByID(acc.id)
                  result <- {
                    if (removed > 0) {
                      val newAccount = acc.copy(password = encryptKey.toSHA256(password))
                      accountService
                        .updateUserAccount(newAccount)
                        .map(x => if (x > 0) Redirect(routes.Default.redirect()) else InternalServerError)
                    }
                    else  Future(InternalServerError)
                  }
                } yield (result)
              }
              .getOrElse(Future(InternalServerError))
          }
        } yield (processed)
      })
  }

  def signUp = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password, code)  =>
        // validate username and referral code..
        for {
          isAccountAlreadyExists <- accountService.getAccountByName(username)
          processSignUp <- {
            if (isAccountAlreadyExists == None) {
              // if (hasCode.referralCode == code.getOrElse("")) {
                try {
                  // encrypt account password into SHA256 algorithm...
                  val userAccount: UserAccount = UserAccount(username, encryptKey.toSHA256(password))
                  // generate User Account, VIP Account and User Wallet..
                  for {
                    _ <- accountService.newUserAcc(userAccount)
                    _ <- accountService.addUpdateUserToken(UserToken(userAccount.id))
                    _ <- accountService.newVIPAcc(VIPUser(userAccount.id, userAccount.createdAt))
                    _ <- accountService.addUserWallet(new UserAccountWallet(userAccount.id, Coin("BTC"), Coin("ETH"), Coin("USDC")))
                    processCode <- {
                      if (code != None) {
                        for {
                          hasCode <- accountService.getAccountByCode(code.getOrElse(""))
                          isUpdated <- {
                            referralHistoryService
                              .applyReferralCode(userAccount.id, hasCode.map(_.referralCode).getOrElse(null))
                              .map(x => if (x > 0) Created else InternalServerError)
                          }
                        } yield (isUpdated)
                      }
                      else Future(Created)
                    }
                  } yield (processCode)
                }
                catch { case _: Throwable => Future(InternalServerError) }
            }
            else Future(Conflict)
          }
        } yield (processSignUp)
      })
  }
  // def signOut() = Action.async { implicit request =>
  //   signForm.bindFromRequest.fold(
  //     formErr => Future.successful(BadRequest("Form Validation Error.")),
  //     { case (username, password)  =>
  //       accountService
  //         .getAccountByUserNamePassword(username, encryptKey.toSHA256(password))
  //         .map {
  //           case Some(account) =>
  //             accountService
  //               .updateUserAccount(account.copy(token=None, tokenLimit=None))
  //               .map(x => if (x > 0) Accepted else InternalServerError)
  //           case _ => Future(Unauthorized(views.html.defaultpages.unauthorized()))
  //         }.flatten
  //     })
  // }
  // TODO: store password in hash256 format...
  def signIn(verify: Boolean) = Action.async { implicit request =>
    signForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        try {
          for {
            // check if sigin is from verifications no need Encryption
            // else Encryptkey for security validation
            isAccountExists <- {
              if (verify) accountService.getAccountByUserNamePassword(username, password)
              else accountService.getAccountByUserNamePassword(username, encryptKey.toSHA256(password))
            }
            processed <- {
              // check if has existing token (UPDATE) else insert new and return to user..
              if (isAccountExists != None)  {
                for {
                  // get account if has existing token..
                  userToken <- accountService.getUserTokenByID(isAccountExists.get.id)
                  // if verify make sure that existing token wont be overrided on this request..
                  hasSession <- {
                    userToken
                      .map { user =>
                        val tempAccount: UserToken = userAction.generateLoginToken(user)
                        val currentTime: Long = Instant.now.getEpochSecond
                        // check if has existing valid token else create new
                        if (user.login.map(_ >= currentTime).getOrElse(false))
                          Future(Ok(ClientTokenEndpoint(user.id, user.token.getOrElse(null)).toJson()))
                        else
                          accountService
                            .updateUserToken(tempAccount)
                            .map { x =>
                              if (x > 0) Ok(ClientTokenEndpoint(user.id, tempAccount.token.getOrElse(null)).toJson())
                              else InternalServerError
                           }
                      }
                      .getOrElse(Future(InternalServerError))
                  }
                } yield (hasSession)
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
        case (u, p, e) => Ok(views.html.emailVerificationTemplate(u, p, e))
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
          account <- accountService.getAccountByEmailAddress(email)
          result <- {
            // update email request limit
            if (account != None) {
              try {
                for {
                  // get user account token
                  userToken <- accountService.getUserTokenByID(account.get.id)
                  // update its password token limit
                  updated <- accountService
                    .updateUserToken(userToken.map(_.copy(password = Some(Config.MAIL_EXPIRATION)))
                    .getOrElse(null))
                  // send email confirmation link
                  result <- {
                    if (updated > 0) mailerService.sendResetPasswordEmail(account.get, email).map(_ => Created)
                    else Future(InternalServerError)
                  }
                } yield (result)
              }
              catch { case _: Throwable => Future(NotFound) }
            }
            else Future(NotFound)
          }
        } yield (result)
      })
  }
  def resetPasswordVerification(code: String) = Action.async { implicit request =>
    try {
      validateEmail.passwordFromCode(code) map {
        case (u, p) => Ok(views.html.resetPasswordTemplate(u))
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
  def getCoinCapAsset() = Action.async { implicit request =>
    multiCurrencySupport.getCoinCapAssets().map(x => Ok(Json.toJson(x)))
  }
   def getAccountNameByID(id: UUID) = Action.async { implicit request =>
    accountService.getAccountByID(id).map(x => Ok(Json.obj("username" -> x.map(_.username))))
  }
}