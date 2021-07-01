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
import models.domain.eosio.{ TreasureHuntGameData, TreasureHuntGameDataPanelSet }
import utils.auth.SecureUserAction
import utils.Config

case class SeqIntForms(sets: Seq[Int])

@Singleton
class GameActionController @Inject()(
                          accountService: UserAccountService,
                          allGameHistoryService: OverAllHistoryService,
                          treasureHuntGameService: TreasureHuntGameService,
                          ghostQuestService: GQGameService,
                          eosioHTTPSupport: utils.lib.EOSIOHTTPSupport,
                          cc: ControllerComponents,
                          SecureUserAction: SecureUserAction) extends AbstractController(cc) {
  private val thGameStartForm = Form(tuple(
    "quantity" -> number,
    "currency" -> nonEmptyText))
  private val thAutoPlayForm = Form(single("sets" -> list(number)))
  private val thOpenTileForm = Form(single("tile" -> number))
  private val thSetEnemyForm = Form(single("enemy" -> number))
  private val thDestinationForm = Form(single("destination" -> number))

  def treasureHuntGameData() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .userData(account.userGameID)
          .map(x => Ok(Json.toJson(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntInitialize() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .initialize(account.userGameID, account.username)
          .map(x => Ok(JsBoolean(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntQuit() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .quit(account.userGameID, account.username)
          .map(x => Ok(JsBoolean(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntSetGamePanel() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .setGamePanel(account.userGameID, account.username)
          .map(x => Ok(JsBoolean(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntSetDestination() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thDestinationForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (destination)  =>
          treasureHuntGameService
            .setDestination(account.userGameID, account.username, destination)
            .map(x => Ok(JsBoolean(x)))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntSetEnemy() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thSetEnemyForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (enemyCount)  =>
          treasureHuntGameService
            .setEnemy(account.userGameID, account.username, enemyCount)
            .map(x => Ok(JsBoolean(x)))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntOpenTile() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thOpenTileForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (tile)  =>
          val gameID: Int = account.userGameID
          for {
            isOpened <- treasureHuntGameService.openTile(gameID, account.username, tile)
            process <- {
              if (isOpened) {
                // return true=win or false=lose
                for {
                  gameData <- treasureHuntGameService.userData(gameID)
                  isWin <- Future.successful {
                    gameData.map { data =>
                      val panelSet: TreasureHuntGameDataPanelSet = data.panel_set(tile)
                      // check tile status and tile result...
                      if (panelSet.iswin == 1 && panelSet.isopen == 1)
                        Ok(Json.obj("is_win" -> true, "status" -> data.status))
                      else
                        Ok(Json.obj("is_win" -> false, "status" -> data.status))
                    }
                    .getOrElse(InternalServerError)
                  }
                } yield (isWin)
              }
              else Future(InternalServerError)
            }
          } yield (process)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntAutoPlay() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thAutoPlayForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (sets)  =>
          treasureHuntGameService
            .autoPlay(account.userGameID, account.username, sets)
            .map(x => Ok(JsBoolean(x)))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntGameStart() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thGameStartForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (quantity, symbol)  =>
          treasureHuntGameService
            .gameStart(account.id, account.userGameID, symbol, quantity)
            .map(x => if (x > 0) Created else InternalServerError)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntWithdraw() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .withdraw(account.id, account.userGameID)
          .map(x => if (x > 0) Created else InternalServerError)
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