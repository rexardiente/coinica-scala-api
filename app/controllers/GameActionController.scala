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
import models.domain.wallet.support.{ Coin, CoinDeposit, CoinWithdraw }
import models.repo._
import models.service._
import utils.auth.SecureUserAction
import utils.Config

@Singleton
class GameActionController @Inject()(
                          accountService: UserAccountService,
                          allGameHistoryService: OverAllHistoryService,
                          ghostQuestService: GQGameService,
                          multiCurrencySupport: utils.lib.MultiCurrencyHTTPSupport,
                          cc: ControllerComponents,
                          SecureUserAction: SecureUserAction) extends AbstractController(cc) {
  private val thGameStartForm = Form(tuple(
    "quantity" -> number,
    "currency" -> nonEmptyText))
  private val thWithdrawForm = Form(single("id" -> number))

  def thGameStart() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thGameStartForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (quantity, symbol)  =>
          accountService
            .thGameStart(account.id, account.userGameID, symbol, quantity)
            .map(x => if (x > 0) Created else InternalServerError)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def thWithdraw() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        // TODO: wait for API that returns amount to withdraw from SC
        ???
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
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

}