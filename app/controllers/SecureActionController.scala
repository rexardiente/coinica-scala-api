package controllers

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDateTime }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import models.domain._
import models.repo._
import models.service._
import utils.auth.SecureUserAction

@Singleton
class SecureActionController @Inject()(
                          accountService: UserAccountService,
                          vipRepo: VIPUserRepo,
                          referralHistory:  ReferralHistoryService,
                          allGameHistoryService: OverAllHistoryService,
                          ghostQuestService: GQGameService,
                          taskService: TaskService,
                          cc: ControllerComponents,
                          SecureUserAction: SecureUserAction) extends AbstractController(cc) {
  private val referralForm = Form(tuple(
    "code" -> nonEmptyText,
    "applied_by" -> uuid))
  private val emailForm = Form(single("email" -> email))
  def addOrUpdateEmailAccount() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        emailForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid Email Address")),
        { case (email)  =>
          accountService
            .addOrUpdateEmailAccount(account.id, email)
            .map(x => if (x > 0) Created else Conflict)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }

  // http://127.0.0.1:9000/donut/api/v1/token/renew
  def renewSessionToken() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        val newUserToken: UserAccount = SecureUserAction.generateToken(account)
        accountService
          .updateUserAccount(newUserToken.copy(lastSignIn = Instant.now))
          .map { x =>
            if (x > 0) Ok(Json.obj("token" -> newUserToken.token, "limit" -> newUserToken.tokenLimit))
            else InternalServerError
          }
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
        if (account.referralCode == code)
          referralHistory.getByCode(code).map(x => Ok(Json.toJson(x)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
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
  // Ghost Quest Game
  // def getAllCharacters() = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map(_ => gQCharacterDataRepo.all().map(x => Ok(Json.toJson(x))))
  //     .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
  def getAllCharactersByUser(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.username == id)
          ghostQuestService.getAllCharactersDataAndHistoryLogsByUser(id).map(Ok(_))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getCharactersByUser(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == id)
          ghostQuestService.getAliveCharacters(id).map(Ok(_))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getCharacterByID(id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.getCharacterDataByID(id).map(Ok(_)))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getCharacterByUserAndID(userID: UUID, id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == userID)
          ghostQuestService.getCharacterByUserAndID(userID, id).map(Ok(_))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getCharacterHistoryByUser(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == id)
          ghostQuestService.getAllEliminatedCharacters(id).map(Ok(_))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getCharacterHistoryByUserAndID(userID: UUID, id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == userID)
          ghostQuestService.getCharacterHistoryByUserAndID(userID, id).map(Ok(_))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getAllGQGameHistory() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.getAllGameHistory().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getGQGameHistoryByUser(id: UUID) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == id)
          ghostQuestService.getGQGameHistoryByUserID(id).map(x => Ok(Json.toJson(x)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getGQGameHistoryByUserAndCharacterID(userID: UUID, id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        if (account.id == userID)
          ghostQuestService.getGameHistoryByUsernameAndCharacterID(userID, id).map(x => Ok(Json.toJson(x)))
        else
          Future(Unauthorized(views.html.defaultpages.unauthorized()))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def getGQGameHistoryByGameID(id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.filteredGameHistoryByID(id).map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def highEarnCharactersAllTime() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersAllTime().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def highEarnCharactersDaily() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersDaily().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def highEarnCharactersWeekly() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersWeekly().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }

  def winStreakPerDay() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakPerDay().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def winStreakPerWeekly() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakPerWeekly().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def winStreakLifeTime() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakLifeTime().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
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