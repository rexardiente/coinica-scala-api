package controllers

import javax.inject.{ Inject, Singleton }
import java.util.UUID
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
import utils.SystemConfig
import utils.lib.GhostQuestEOSIO

@Singleton
class GameActionController @Inject()(
                          accountService: UserAccountService,
                          allGameHistoryService: OverAllHistoryService,
                          treasureHuntGameService: TreasureHuntGameService,
                          ghostQuestService: GhostQuestGameService,
                          mjHiloGameService: MahjongHiloGameService,
                          ghostQuestEOSIO: GhostQuestEOSIO,
                          cc: ControllerComponents,
                          SecureUserAction: SecureUserAction) extends AbstractController(cc) {
  private val thInitForm = Form(tuple(
    "currency" -> nonEmptyText,
    "quantity" -> number,
    "destination" -> number,
    "enemy" -> number))
  private val thAutoPlayForm = Form(single("sets" -> list(number)))
  private val thOpenTileForm = Form(single("tile" -> number))
  private val mjHiloDeclareKongForm = Form(single("sets" -> list(number)))
  private val mjHiloDiscardTileForm = Form(single("tile" -> number))
  private val mjHiloPlayHiloForm = Form(single("option" -> number))
  private val mjHiloAddBetForm = Form(tuple("currency" -> nonEmptyText, "quantity" -> number))
  private val ghostQuestGenerateCharacterForm = Form(tuple("currency" -> nonEmptyText, "quantity" -> number, "limit" -> number))
  private val ghostQuestKeyForm = Form(single("key" -> nonEmptyText))

  def mahjongHiloGetMonthlyRanking() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getMonthlyRanking()
          .map(x => Ok(Json.toJson(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetHiLoWinRate() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getHiLoWinRate(account.userGameID)
          .map(x => Ok(JsNumber(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetTotalPlayed() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getTotalPlayed(account.userGameID)
          .map(x => Ok(JsNumber(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetConsecutiveHilo() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getMaxConsecutiveHilo(account.userGameID)
          .map(x => Ok(JsNumber(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetMaxPayout() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getMaxPayout(account.userGameID)
          .map(x => Ok(JsNumber(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloDeclareWinHand() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .declareWinHand(account.userGameID)
          .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloDeclareKong() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloDeclareKongForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (sets)  =>
          mjHiloGameService
            .declareKong(account.userGameID, sets)
            .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloDiscardTile() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloDiscardTileForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (tile)  =>
          mjHiloGameService
            .discardTile(account.userGameID, tile)
            .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloPlayHilo() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloPlayHiloForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (option)  =>
          for {
            (isWin, hash, gameData) <- mjHiloGameService.playHilo(account.id, account.username, account.userGameID, option)
            process <- Future.successful {
              if (isWin <= 2) {
                gameData
                  .map(v => Ok(v.toJson.as[JsObject] + ("transaction_id" -> JsString(hash))))
                  .getOrElse(InternalServerError)
              }
              else InternalServerError
            }
          } yield (process)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloRiichiDiscard() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .riichiDiscard(account.userGameID)
          .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloInitialize() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .initialize(account.userGameID)
          .map(x => if (x > 0) Ok(JsBoolean(true)) else InternalServerError)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloEnd() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .end(account.userGameID, account.username)
          .map(x => if (x > 0) Ok(JsBoolean(true)) else InternalServerError)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloResetBet() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .resetBet(account.userGameID)
          .map(x => if (x > 0) Ok(JsBoolean(true)) else InternalServerError)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloAddBet = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloAddBetForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (currency, quantity)  =>
          mjHiloGameService
            .addBet(account.id, account.userGameID, currency.toUpperCase, quantity)
            .map(x => if (x > 0) Created else InternalServerError)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloStart = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .start(account.userGameID)
          .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloTransfer = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .transfer(account.userGameID)
          .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloWithdraw = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .withdraw(account.id, account.username, account.userGameID)
          .map(x => if (x._1) Ok(Json.obj("transaction_id" -> x._2)) else InternalServerError)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetUserData = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getUserData(account.userGameID)
          .map(x => Ok(Json.toJson(x)))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def mahjongHiloGetUserGameHistory(limit: Int) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        mjHiloGameService
          .getUserGameHistory(account.userGameID, limit)
          .map(x => Ok(Json.toJson(x.map(_.toTrimmedJson()))))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
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
        thInitForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (currency, quantity, destination, enemy)  =>
          treasureHuntGameService
            .initialize(account.id, account.userGameID, currency.toUpperCase, quantity, destination, enemy)
            .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntQuit() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .quit(account.userGameID, account.username)
          .map(_.map(x => Ok(JsString(x))).getOrElse(InternalServerError))
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntOpenTile() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        thOpenTileForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (tile)  =>
          for {
            (isWin, hash, gameData) <- treasureHuntGameService.openTile(account.id, account.userGameID, account.username, tile)
            process <- Future.successful {
              // isWin = 1 is win
              // isWin = 2 is lost
              // return true=win or false=lose
              if (isWin <= 2) {
                gameData
                  .map(v => Ok(v.toJson.as[JsObject] + ("transaction_id" -> JsString(hash))))
                  .getOrElse(InternalServerError)
              }
              // isWin = 3 server error
              else InternalServerError
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
          for {
            (isWin, hash, gameData) <- treasureHuntGameService.autoPlay(account.id, account.userGameID, account.username, sets)
            process <- Future.successful {
              // isWin = 1 is win
              // isWin = 2 is lost
              // return true=win or false=lose
              if (isWin <= 2) {
                gameData
                  .map(v => Ok(v.toJson.as[JsObject] + ("transaction_id" -> JsString(hash))))
                  .getOrElse(InternalServerError)
              }
              // isWin = 3 server error
              else InternalServerError
            }
          } yield (process)
        })
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def treasureHuntWithdraw() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        treasureHuntGameService
          .withdraw(account.id, account.username, account.userGameID)
          .map(x => if (x._1) Ok(Json.obj("transaction_id" -> x._2)) else InternalServerError)
      }.getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetAllGQGameHistory() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.getAllGameHistory().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetGQGameHistoryByOwnerIDAndCharacterID(id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestService
          .getGameHistoryByGameIDAndCharacterID(id, account.userGameID)
          .map(x => Ok(Json.toJson(x)))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetGQGameHistoryByGameID(id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.filteredGameHistoryByID(id).map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetAllCharactersByUser() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestService.getAllCharactersDataAndHistoryLogsByUser(account.id, account.userGameID).map(Ok(_))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetAliveCharactersByUser() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(account => ghostQuestService.getAliveCharacters(account.userGameID).map(Ok(_)))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  // def ghostQuestGetCharacterByID(key: String) = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map(account => ghostQuestService.getCharacterDataByID(account.userGameID, key).map(Ok(_)))
  //     .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
  def getCharactInfoByOwnerIDAndKey(id: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(account => ghostQuestService.getCharactInfoByKey(id).map(Ok(_)))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetCharacterHistoryByUser() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(account => ghostQuestService.getAllEliminatedCharacters(account.userGameID).map(Ok(_)))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetCharacterHistoryByUserAndID(key: String) = SecureUserAction.async { implicit request =>
    request
      .account
      .map(account => ghostQuestService.getGhostQuestCharacterHistoryByOwnerIDAndID(account.userGameID, key).map(Ok(_)))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestHighEarnCharactersAllTime() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersAllTime().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestHighEarnCharactersDaily() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersDaily().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestHighEarnCharactersWeekly() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.highEarnCharactersWeekly().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestWinStreakPerDay() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakPerDay().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestWinStreakPerWeekly() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakPerWeekly().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestWinStreakLifeTime() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(_ => ghostQuestService.winStreakLifeTime().map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGetUserData() = SecureUserAction.async { implicit request =>
    request
      .account
      .map(account => ghostQuestService.getUserData(account.userGameID).map(x => Ok(Json.toJson(x))))
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  // def ghostQuestGetAllCharacters() = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map(account => ghostQuestService.getAllCharacters().map(x => Ok(Json.toJson(x))))
  //     .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
  def ghostQuestInitialize() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestService
          .initialize(account.userGameID, account.username)
          .map(x => Ok(JsString(x.getOrElse(null))))
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestGenerateCharacter() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestGenerateCharacterForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (currency, quantity, limit)  =>
          ghostQuestService
            .generateCharacter(account.id, account.username, account.userGameID, currency.toUpperCase, quantity, limit)
            .map(x => if (x > 0) Created else InternalServerError)
        })
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestAddLife() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestKeyForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (key)  =>
          ghostQuestService
            .addLife(account.userGameID, key)
            .map(x => Ok(JsString(x.getOrElse(null))))
        })
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  def ghostQuestWithdraw() = SecureUserAction.async { implicit request =>
    request
      .account
      .map { account =>
        ghostQuestKeyForm.bindFromRequest.fold(
        formErr => Future.successful(BadRequest("Invalid request")),
        { case (key)  =>
          ghostQuestService
            .withdraw(account.id, account.username, account.userGameID, key)
            .map(x => if (x._1) Ok(Json.obj("transaction_id" -> x._2)) else InternalServerError)
        })
      }
      .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  }
  // def ghostQuestEliminate() = SecureUserAction.async { implicit request =>
  //   request
  //     .account
  //     .map { account =>
  //       ghostQuestKeyForm.bindFromRequest.fold(
  //       formErr => Future.successful(BadRequest("Invalid request")),
  //       { case (key)  =>
  //         ghostQuestService
  //           .eliminate(account.userGameID, key)
  //           .map(x => Ok(JsString(x.getOrElse(null))))
  //       })
  //     }
  //     .getOrElse(Future(Unauthorized(views.html.defaultpages.unauthorized())))
  // }
}