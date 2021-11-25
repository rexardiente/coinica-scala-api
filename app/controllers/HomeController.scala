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
import models.domain.wallet.support.Coin
import akka.WebSocketActor
import utils.SystemConfig.{ COINICA_WEB_HOST, SUPPORTED_SYMBOLS }
import utils.auth.AccountTokenSession.{ LOGIN, UPDATE_EMAIL, RESET_PASSWORD }
import utils.auth.SecureUserAction
import utils.lib.MultiCurrencyHTTPSupport
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      platformConfigService: PlatformConfigService,
      vipUserRepo: VIPUserRepo,
      accountService: UserAccountService,
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      challengeRepo: ChallengeRepo,
      newsRepo: NewsRepo,
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
      SecureUserAction: SecureUserAction,
      encryptKey: utils.auth.EncryptKey,
      validateEmail: EmailValidation,
      mailerService: MailerService,
      multiCurrencySupport: MultiCurrencyHTTPSupport,
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
  private val emailForm = Form(single("email" -> email.verifying(play.api.data.validation.Constraints.emailAddress)))
  private val resetPasswordForm = Form(tuple(
    "accountid" -> uuid,
    "new_password" -> nonEmptyText,
    "confirm_password" -> nonEmptyText
  ).verifying("Password and Confirm password does not match", info => info._2 == info._3))


  def socket = WebSocket.accept[Event, Event] { implicit req =>
    play.api.libs.streams.ActorFlow.actorRef { out =>
      WebSocketActor.props(out, platformConfigService, accountService, overAllGameHistoryRepo, vipUserRepo, ghostQuestEOSIO, dynamicBroadcast, dynamicProcessor)
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
      { case (accountid, password, confirm)  =>
        for {
          // check if account has requested to reset password by accountid and still valid expiration time
          hasSession <- Future.successful {
            RESET_PASSWORD.filter(x => x._1 == accountid && x._2._2 >= Instant.now.getEpochSecond).headOption
          }
          // if process successful, you can now remove the session token
          _ <- Future.successful(RESET_PASSWORD.remove(accountid))
          // update account password if exists
          isUpdated <- {
            hasSession
              .map { session =>
                // all validation are success, we can now proceed updating the user account password..
                  for {
                    hasAccount <- accountService.getAccountByID(accountid)
                    updateAccount <- {
                      hasAccount
                        .map(account => accountService.updateUserAccount(account.copy(password = encryptKey.toSHA256(password))))
                        .getOrElse(Future.successful(0))
                    }
                  } yield (updateAccount)
              }
              .getOrElse(Future.successful(0))
          }
          isDone <- Future.successful(if (isUpdated > 0) Redirect(COINICA_WEB_HOST) else NotFound)
        } yield (isDone)
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
                    _ <- accountService.newVIPAcc(VIPUser(userAccount.id, userAccount.createdAt))
                    _ <- accountService.addUserWallet(new UserAccountWallet(
                            userAccount.id,
                            Coin(SUPPORTED_SYMBOLS(2)),
                            Coin(SUPPORTED_SYMBOLS(1)),
                            Coin(SUPPORTED_SYMBOLS(0))))
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
  // TODO: store password in hash256 format...
  def signIn = Action.async { implicit request =>
    signForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (username, password)  =>
        try {
          for {
            isAccountExists <- accountService.getAccountByUserNamePassword(username, encryptKey.toSHA256(password))
            processed <- {
              // check if has existing token (UPDATE) else insert new and return to user..
              isAccountExists.map { account =>
                val accountID: UUID = account.id
                for {
                  // get account if has existing token..
                  userToken <- Future.successful(LOGIN.filter(_._1 == accountID).headOption)
                  generateToken <- SecureUserAction.generateToken()
                  // if verify make sure that existing token wont be overrided on this request..
                  verifyToken <- {
                    userToken
                      .map { session =>
                        // check if has existing then extend expiration time..
                        val currentTime: Long = Instant.now.getEpochSecond
                        // reuse and do not update sessions to avoid spam
                        if (session._2._2 >= currentTime) {
                          // LOGIN(session._1) = (session._2._1, newToken._2)
                          Future.successful(Ok(ClientTokenEndpoint(accountID, session._2._1).toJson()))
                        }
                        // use the newly generated token and expiration time
                        else {
                          val newToken: (String, Long) = generateToken
                          LOGIN(session._1) = newToken
                          Future.successful(Ok(ClientTokenEndpoint(accountID, newToken._1).toJson()))
                        }
                      }
                      .getOrElse({
                        val newToken: (String, Long) = generateToken
                        LOGIN.addOne(accountID -> newToken)
                        Future.successful(Ok(ClientTokenEndpoint(accountID, newToken._1).toJson()))
                      })
                  }
                } yield (verifyToken)
              }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
            }
          } yield (processed)
        }
        catch { case _: Throwable => Future(InternalServerError) }
      })
  }
  def emailVerificationSubmit(id: UUID, code: String) = Action.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (email)  =>
        for {
          // check if user has existing change email token
          hasSession <- Future.successful(validateEmail.addOrUpdateEmailFromCode(id, code))
          isDone <- {
            if (hasSession) {
              for {
                // get account details
                hasAccount <- accountService.getAccountByID(id)
                _ <- Future.successful{
                  // remove session details and send notification to UI
                  UPDATE_EMAIL.remove(id)
                  dynamicBroadcast ! ("BROADCAST_EMAIL_UPDATED", id, email)
                }
                // update account
                updateAccount <- {
                  hasAccount.map { account =>
                    val updatedAccount = account.copy(email = Some(email), isVerified = true)
                    accountService
                      .updateUserAccount(updatedAccount)
                      .map(x => if (x > 0) Created else InternalServerError)
                  }
                  .getOrElse(Future.successful(NotFound))
                }
              } yield (updateAccount)
            }
            else Future.successful(NotFound)
          }
        } yield (isDone)
      })
  }
  def emailVerification(id: UUID, email: String, code: String) = Action.async { implicit request =>
    Future.successful(Ok(views.html.emailVerificationTemplate(id, email, code)))
  }
  // Check if email is valid email and if email exist then send confirmation link..
  def resetPassword() = Action.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (email)  =>
        for {
          account <- accountService.getAccountByEmailAddress(email)
          generateToken <- SecureUserAction.generateToken()
          result <- {
            // update email request limit
            if (account != None) {
              val acc = account.get
              // generate new token for reset password
              val newToken: (String, Long) = generateToken
              for {
                // check if has existing request token to update else create new
                _ <- Future.successful {
                  // add the newly created token to session lists..
                  RESET_PASSWORD
                    .filter(_._1 == acc.id)
                    .headOption
                    .map(session => RESET_PASSWORD(session._1) = newToken)
                    .getOrElse(RESET_PASSWORD.addOne(acc.id -> newToken))
                }
                send <- Future.successful {
                  try {
                    mailerService.sendResetPasswordEmail(acc.id, acc.username, email, newToken)
                    Created
                  }
                  catch { case _: Throwable => InternalServerError }
                }
              } yield (send)
            }
            else Future(NotFound)
          }
        } yield (result)
      })
  }
  def resetPasswordVerification(id: UUID, code: String) = Action.async { implicit request =>
    for {
      // check if user has existing change email token
      hasSession <- Future.successful(validateEmail.resetPasswordFromCode(id, code))
      isDone <- {
        if (hasSession) {
          for {
            // get account details
            hasAccount <- accountService.getAccountByID(id)
            // update account
            updateAccount <- Future.successful {
              hasAccount
                .map(_ => Ok(views.html.resetPasswordTemplate(id)))
                .getOrElse(NotFound)
            }
          } yield (updateAccount)
        }
        else Future.successful(NotFound)
      }
    } yield (isDone)
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

  def getChallenge(date: Option[Instant]) = Action.async { implicit req =>
    challengeService.getChallenge(date).map(Ok(_))
  }

  def getDailyRanksChallenge() = Action.async { implicit req =>
    challengeService.getDailyRanksChallenge.map(Ok(_))
  }

  def getRankingDaily() = Action.async { implicit req =>
    rankingService.getRankingDaily().map(x => Ok(Json.toJson(x)))
  }
  def getRankingHistory() = Action.async { implicit req =>
    rankingService.getRankingHistory().map(x => Ok(Json.toJson(x)))
  }
  def games() = Action.async { implicit req =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }

  def findGameByID(id: UUID) = Action.async { implicit req =>
    gameRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  /* GENRE API */
  def genres() = Action.async { implicit req =>
    genreRepo.all().map(genre => Ok(Json.toJson(genre)))
  }
  def findGenreByID(id: UUID) = Action.async { implicit req =>
    genreRepo.findByID(id).map(game => Ok(Json.toJson(game)))
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
}