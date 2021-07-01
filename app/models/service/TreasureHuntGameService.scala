package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
// import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import utils.Config.SUPPORTED_SYMBOLS
import models.domain.eosio.TreasureHuntGameData
// import models.repo.eosio._

@Singleton
class TreasureHuntGameService @Inject()(contract: utils.lib.EOSIOHTTPSupport,
																				userAccountService: UserAccountService,
																				overAllHistory: OverAllHistoryService) {
	def userData(gameID: Int): Future[Option[TreasureHuntGameData]] =
		contract.treasureHuntGetUserData(gameID)
	def autoPlay(gameID: Int, username: String, sets: Seq[Int]): Future[Boolean] =
		contract.treasureHuntAutoPlay(gameID, username, sets)
	def openTile(gameID: Int, username: String, index: Int): Future[Boolean] =
		contract.treasureHuntOpenTile(gameID, username, index)
	def setEnemy(gameID: Int, username: String, count: Int): Future[Boolean] =
		contract.treasureHuntSetEnemy(gameID, username, count)
	def setDestination(gameID: Int, username: String, destination: Int): Future[Boolean] =
		contract.treasureHuntSetDestination(gameID, username, destination)
	def setGamePanel(gameID: Int, username: String): Future[Boolean] =
		contract.treasureHuntSetGamePanel(gameID, username)
	def quit(gameID: Int, username: String): Future[Boolean] =
		contract.treasureHuntQuit(gameID, username)
	def initialize(gameID: Int, username: String): Future[Boolean] =
		contract.treasureHuntInitialize(gameID, username)
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
      getPrize <- Future.successful(gameData.map(_.prize).getOrElse(BigDecimal(0)))
      processWithdraw <- {
      	if (getPrize > 0) contract.treasureHuntWithdraw(gameID)
      	else Future(false)
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	hasWallet.map { _ =>
      		if (processWithdraw) userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize)
      		else Future(0)
      	}
      	.getOrElse(Future(0))
      }
    } yield (updateBalance)
	}
}