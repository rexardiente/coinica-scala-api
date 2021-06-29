package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
// import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
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
	def withdraw(gameID: Int) = {
		contract.treasureHuntWithdraw(gameID)
	}
}