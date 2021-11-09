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
import play.api.libs.mailer.MailerService
import play.api.data.validation.Constraints.emailAddress
import models.domain._
import models.domain.wallet.support.{ Coin, CoinDeposit, CoinWithdraw }
import models.repo._
import models.service._
import utils.auth.SecureUserAction
import utils.auth.AccountTokenSession.{ LOGIN, ADD_OR_RESET_EMAIL }
import utils.Config

@Singleton
class SecureActionController @Inject()(
                          accountService: UserAccountService,
                          vipRepo: VIPUserRepo,
                          referralHistory:  ReferralHistoryService,
                          allGameHistoryService: OverAllHistoryService,
                          taskService: TaskService,
                          mailerService: MailerService,
                          multiCurrencySupport: utils.lib.MultiCurrencyHTTPSupport,
                          cc: ControllerComponents,
                          SecureUserAction: SecureUserAction) extends AbstractController(cc) with utils.CommonImplicits {
  private val referralForm = Form(tuple(
    "code" -> nonEmptyText,
    "applied_by" -> uuid))
  private val emailForm = Form(single("email" -> email.verifying(emailAddress)))
  private val getSupportedSymbolForm = Form(single("symbol" -> text))
  private val getListOfOrdersForm = Form(tuple(
    "start" -> optional(number),
    "count" -> optional(number),
    "id" -> optional(text)))
  private val getExchangeLimitsForm = Form(tuple(
    "deposit" -> nonEmptyText,
    "destination" -> nonEmptyText))
  private val generateOfferForm = Form(tuple(
    "deposit" -> nonEmptyText,
    "destination" -> nonEmptyText,
    "amount" -> bigDecimal))
  // https://stackoverflow.com/questions/15074684/play-framework-2-1-form-mapping-with-complex-objects
  // https://stackoverflow.com/questions/12850000/how-can-i-handle-decimal-numbers-using-the-scala-framework-play
  private val depositForm = Form(mapping(
    "tx_hash" -> nonEmptyText,
    "issuer" -> mapping(
        "address" -> optional(text),
        "currency" -> nonEmptyText,
        "amount" -> bigDecimal
      )(Coin.apply)(Coin.unapply),
    "receiver" -> mapping(
        "address" -> optional(text),
        "currency" -> nonEmptyText,
        "amount" -> bigDecimal
      )(Coin.apply)(Coin.unapply)
    )(CoinDeposit.apply)(CoinDeposit.unapply))
  private val withdrawForm = Form(mapping(
    "receiver" -> mapping(
        "address" -> optional(text),
        "currency" -> nonEmptyText,
        "amount" -> bigDecimal
      )(Coin.apply)(Coin.unapply),
    "fee" -> bigDecimal
    )(CoinWithdraw.apply)(CoinWithdraw.unapply))

  def coinDeposit() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        depositForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case deposit  =>
          accountService
            .updateWithDepositCoin(account.id, deposit)
            .map(x => if (x > 0) Created else InternalServerError)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }

  def coinWithdraw() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        withdrawForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case withdraw  =>
          val hasExistingTx = akka.SystemSchedulerActor.walletTransactions.filter(_._2.account_id == Some(account.id))
          if (hasExistingTx.isEmpty)
            accountService
              .updateWithWithdrawCoin(account.id, withdraw)
              .map(x => if (x > 0) Created else InternalServerError)
          else Future(Conflict)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }

  def getUserAccountWallet() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        accountService
          .getUserAccountWallet(account.id)
          .map(x => Ok(x.map(_.toJson).getOrElse(JsNull)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getUserAccountWalletHistory() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        accountService
          .getUserAccountWalletHistory(account.id)
          .map(x => Ok(Json.toJson(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def signOut() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        for {
          hasSession <- Future.successful(LOGIN.filter(_._1 == account.id).headOption)
          isDone <- Future.successful {
            hasSession
              .map { session =>
                LOGIN.remove(account.id)
                Accepted
              }
              .getOrElse(NotFound)
          }
        } yield (isDone)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  // def updateEmailAccount() = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map { account =>
  //       emailForm.bindFromRequest.fold(
  //       formErr => Future.successful(BadRequest("Invalid Email Address")),
  //       { case (email)  =>
  //         for {
  //           // remove reset email token
  //           removed <- accountService.removeEmailTokenByID(account.id)
  //           result <- {
  //             if (removed > 0)
  //               accountService
  //                 .addOrUpdateEmailAccount(account.id, email)
  //                 .map(x => if (x > 0) Redirect(routes.HomeController.index()) else Conflict)
  //             else  Future(InternalServerError)
  //           }
  //         } yield (result)
  //       })
  //     }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
  def addOrUpdateEmailAccount() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        emailForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid Email Address")),
        { case (email)  =>
          if (Some(email) == account.email) Future.successful(Conflict)
          else {
            val newToken: (String, Long) = SecureUserAction.generateToken()
              for {
                // check if has existing request token to update else create new
                _ <- Future.successful {
                  // add the newly created token to session lists..
                  ADD_OR_RESET_EMAIL
                    .filter(_._1 == account.id)
                    .headOption
                    .map(session => ADD_OR_RESET_EMAIL(session._1) = newToken)
                    .getOrElse(ADD_OR_RESET_EMAIL.addOne(account.id -> newToken))
                }
                send <- Future.successful {
                  try {
                    mailerService.sendAddOrUpdateEmailAddres(account.id,
                                                            account.username,
                                                            email,
                                                            newToken,
                                                            account.email.map(_ => true).getOrElse(false))
                    Created
                  }
                  catch { case _: Throwable => InternalServerError }
                }
              } yield (send)
          }
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def applyReferralCode() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        referralForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Form Validation Error.")),
        { case (code, appliedBy)  =>
          try {
            referralHistory
              .applyReferralCode(appliedBy, code)
              .map(r => if(r < 1) Forbidden else Created )
          } catch {
            case _: Throwable => Future(InternalServerError)
          }
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getReferralHistory(code: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        for {
          // change referralBy ID to username
          refers <- referralHistory.getByCodeAndReferrentID(account.referralCode, account.id)
          acc <- Future.sequence {
            refers.map { data =>
              accountService
                .getAccountByID(data.applied_by)
                .map(x => data.toReferralHistoryJSON(x.map(_.username).getOrElse(null)))
            }
          }
        } yield (Ok(Json.toJson(acc)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getTodayTaskUpdates(gameID: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        taskService.getTodayTaskUpdates(account.id, gameID).map(_.map(x => Ok(x.toJson)).getOrElse(Ok(JsNull)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getMonthlyTaskUpdates(gameID: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        taskService.getTodayTaskUpdates(account.id, gameID).map(x => Ok(Json.toJson(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  // def getAccountNameByID(id: UUID) = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map { account =>
  //         accountService
  //           .getAccountByID(id)
  //           .map(x => Ok(Json.obj("username" -> x.map(_.username))))
  //     }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
  def getAccountByName(username: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.username == username)
          accountService.getAccountByName(username).map(x => Ok(x.map(Json.toJson(_)).getOrElse(JsNull)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getAccountByID(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == id)
          accountService.getAccountByID(id).map(x => Ok(x.map(Json.toJson(_)).getOrElse(JsNull)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getAccountByCode(code: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.referralCode == code)
          accountService.getAccountByCode(code).map(x => Ok(x.map(Json.toJson(_)).getOrElse(JsNull)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def vipUser(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == id)
          vipRepo.findByID(id).map(x => Ok(x.map(Json.toJson(_)).getOrElse(JsNull)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def overAllHistory(limit: Int) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => allGameHistoryService.all(limit).map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def overAllHistoryByGameID(game: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => allGameHistoryService.gameHistoryByGameID(game).map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def gameHistoryByGameIDAndUser(game: UUID, userID: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == userID)
          allGameHistoryService.gameHistoryByGameIDAndUser(userID, game).map(x => Ok(Json.toJson(x)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
}