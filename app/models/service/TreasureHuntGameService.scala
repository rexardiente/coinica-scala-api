package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
// import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import utils.Config.SUPPORTED_SYMBOLS
// import models.domain.eosio._
// import models.repo.eosio._

@Singleton
class TreasureHuntGameService @Inject()(contract: utils.lib.EOSIOHTTPSupport,
																				userAccountService: UserAccountService,
																				overAllHistory: OverAllHistoryService) {
	def userData(gameID: Int): Future[Option[JsValue]] = {
		contract.treasureHuntGetUserData(gameID)
	}
	def autoPlay(gameID: Int, username: String, sets: Seq[Int]) = {
		contract.treasureHuntAutoPlay(gameID, username, sets)
	}
	def openTile(gameID: Int, username: String, index: Int) = {
		contract.treasureHuntOpenTile(gameID, username, index)
	}
	def setEnemy(gameID: Int, username: String, count: Int) = {
		contract.treasureHuntSetEnemy(gameID, username, count)
	}
	def setDestination(gameID: Int, username: String, destination: Int) = {
		contract.treasureHuntSetDestination(gameID, username, destination)
	}
	def setGamePanel(gameID: Int, username: String) = {
		contract.treasureHuntSetGamePanel(gameID, username)
	}
	def quit(gameID: Int, username: String) = {
		contract.treasureHuntQuit(gameID, username)
	}
	def initialize(gameID: Int, username: String) = {
		contract.treasureHuntInitialize(gameID, username)
	}
	def gameStart(id: UUID, gameID: Int, currency: String, quantity: Int): Future[Int] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      currentValue <- userAccountService.getGameQuantityAmount(currency, quantity)
      // check if has enough balance..
      hasEnoughBalance <- Future.successful {
        hasWallet
          .map(v => userAccountService.hasEnoughBalanceByCurrency(v, currency, currentValue))
          .getOrElse(false)
      }
      // if has enough balance send tx on smartcontract, else do nothing
      initGame <- {
        if (hasEnoughBalance) contract.treasureHuntGameStart(gameID, quantity)
        else Future(false)
      }
      // deduct balance on the account
      updateBalance <- {
        if (initGame) userAccountService.deductBalanceByCurrency(id, currency, currentValue)
        else Future(0)
      }
    } yield (updateBalance)
	}
	def withdraw(id: UUID, gameID: Int): Future[Int] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- contract.treasureHuntGetUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful {
      	gameData.map(js => (js \ "data" \ "game_data" \ "prize").as[BigDecimal])
      	.getOrElse(BigDecimal(0))
      }
      processWithdraw <- {
      	if (getPrize > 0) contract.treasureHuntWithdraw(gameID)
      	else Future(false)
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	if (processWithdraw) userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize)
      	else Future(0)
      }
    } yield (updateBalance)
	}
}