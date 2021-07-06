package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import utils.Config.SUPPORTED_SYMBOLS

@Singleton
class MahjongHiloGameService @Inject()(contract: utils.lib.MahjongHiloEOSIO,
																			userAccountService: UserAccountService,
																			overAllHistory: OverAllHistoryService) {
	def declareWinHand(gameID: Int): Future[Boolean] =
		contract.declareWinHand(gameID)

	def declareKong(gameID: Int, sets: Seq[Int]): Future[Boolean] =
		contract.declareKong(gameID, sets)

	def discardTile(gameID: Int, index: Int): Future[Boolean] =
		contract.discardTile(gameID, index)

	def playHilo(gameID: Int, option: Int): Future[(Boolean, String)] = {
		contract.playHilo(gameID, option).map(_.getOrElse((false, null)))
	}

	def initialize(gameID: Int): Future[Boolean] =
		contract.initialize(gameID)

	def reset(gameID: Int): Future[Boolean] =
		contract.reset(gameID)

	def quit(gameID: Int): Future[Boolean] =
		contract.quit(gameID)

	def addBet(id: UUID, gameID: Int, currency: String, quantity: Int): Future[Int] = {
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
      isAdded <- {
        if (hasEnoughBalance) contract.addBet(gameID, quantity)
        else Future(false)
      }
      // deduct balance on the account
      updateBalance <- {
        if (isAdded) userAccountService.deductBalanceByCurrency(id, currency, currentValue)
        else Future(0)
      }
    } yield (updateBalance)
	}

	def start(gameID: Int): Future[Boolean] =
		contract.start(gameID)

	def transfer(gameID: Int): Future[Boolean] =
		contract.transfer(gameID)

	def withdraw(id: UUID, gameID: Int): Future[Int] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- contract.getUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful {
      	gameData.map(js => (js \ "hi_lo_balance").asOpt[BigDecimal].getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
      }
      processWithdraw <- {
      	if (getPrize > 0) contract.withdraw(gameID)
      	else Future(false, null)
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	hasWallet.map { _ =>
      		if (processWithdraw._1) userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize)
      		else Future(0)
      	}
      	.getOrElse(Future(0))
      }
    } yield (updateBalance)
	}

	def getUserData(gameID: Int): Future[Option[JsValue]] =
		contract.getUserData(gameID)

}