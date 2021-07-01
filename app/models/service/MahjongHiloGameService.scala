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
	def declareWinHand(id: Int): Future[Boolean] =
		contract.declareWinHand(id)

	def declareKong(id: Int, sets: Seq[Int]): Future[Boolean] =
		contract.declareKong(id, sets)

	def discardTile(id: Int, index: Int): Future[Boolean] =
		contract.discardTile(id, index)

	def playHilo(id: Int, option: Int): Future[Boolean] =
		contract.playHilo(id, option)

	def initialize(id: Int): Future[Boolean] =
		contract.initialize(id)

	def reset(id: Int): Future[Boolean] =
		contract.reset(id)

	def quit(id: Int): Future[Boolean] =
		contract.quit(id)

	def addBet(id: Int, quantity: Int): Future[Boolean] =
		contract.addBet(id, quantity)

	def start(id: Int): Future[Boolean] =
		contract.start(id)

	def transfer(id: Int): Future[Boolean] =
		contract.transfer(id)

	def withdraw(id: Int): Future[Boolean] =
		contract.withdraw(id)

	def getUserData(id: Int): Future[Option[JsValue]] =
		contract.getUserData(id)

}