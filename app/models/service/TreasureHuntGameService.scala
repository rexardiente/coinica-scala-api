package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorRef
import play.api.libs.json._
import utils.Config.{ SUPPORTED_SYMBOLS, TH_CODE, TH_GAME_ID }
import models.domain.eosio.TreasureHuntGameData
import models.domain._

@Singleton
class TreasureHuntGameService @Inject()(contract: utils.lib.TreasureHuntEOSIO,
																				userAccountService: UserAccountService,
																				overAllHistory: OverAllHistoryService,
																				@Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
																				@Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
	def userData(gameID: Int): Future[Option[TreasureHuntGameData]] =
		contract.treasureHuntGetUserData(gameID)
	def autoPlay(gameID: Int, username: String, sets: Seq[Int]): Future[Boolean] =
		contract.treasureHuntAutoPlay(gameID, username, sets)
	def openTile(accID: UUID, gameID: Int, username: String, index: Int): Future[(Int, String, Option[TreasureHuntGameData])] = {
		for {
			// return None if failed tx, Option[(Boolean, String)] if success:
			// true = win and false = lose
			isOpenTile <- contract.treasureHuntOpenTile(gameID, username, index)
			gameData <- userData(gameID)
			// if lost then save to DB, else do nothing
			onProcess <- {
				isOpenTile
					.map { hash =>
						// if process succesful then proceed checking if win or lose
						gameData
							.map { data =>
								// check if the status is DONE, then save to DB
								if (data.status == 2) {
									for {
										// check if tx_hash already exists to DB history
										isExists <- overAllHistory.gameIsExistsByTxHash(hash)
										// return true if successful adding to DB else false
										onSaveHistory <- {
											if (!isExists) {
												val gameID: String = data.game_id
												val prediction: List[Int] = data.panel_set.map(_.isopen).toList
					              val result: List[Int] = data.panel_set.map(_.iswin).toList
					              val betAmount: Double = data.destination
					              val prize: Double = 0
												// create OverAllGameHistory object..
												val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
					                                                                      hash,
					                                                                      gameID,
					                                                                      TH_CODE,
					                                                                      ListOfIntPredictions(accID,
								                                                                                    prediction,
								                                                                                    result,
								                                                                                    betAmount,
								                                                                                    prize),
					                                                                      true,
					                                                                      Instant.now.getEpochSecond)

												overAllHistory.gameAdd(gameHistory)
													.map { isAdded =>
									          if (isAdded > 0) {
									          	dynamicBroadcast ! Array(gameHistory)
										          dynamicProcessor ! DailyTask(accID, TH_GAME_ID, 1)
															dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, 0)
															(2)
									          }
									          else (3)
										      }
											}
											else Future(3)
										}
									} yield (onSaveHistory)
								}
								else Future(1) // return 1 if WIN
							}
							.getOrElse(Future(3))
					}
					.getOrElse(Future(3))
			}
		} yield (onProcess, isOpenTile.getOrElse(null), gameData)
	}
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
	def withdraw(id: UUID, gameID: Int): Future[(Boolean, String)] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- contract.treasureHuntGetUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful(gameData.map(_.prize).getOrElse(BigDecimal(0)))
      processWithdraw <- {
      	if (getPrize > 0) contract.treasureHuntWithdraw(gameID)
      	else Future(None)
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	hasWallet.map { _ =>
      		processWithdraw
      			.map(_ => userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize))
      			.getOrElse(Future(0))
      	}
      	.getOrElse(Future(0))
      }
      isSaveHistory <- {
      	if (updateBalance > 0) {
      		processWithdraw
      			.map { txHash =>
      				gameData
      					.map { data =>
      						val accID: UUID = hasWallet.map(_.id).getOrElse(UUID.randomUUID)
      						for {
										isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
										processedHistory <- {
											if (!isExists) {
												val gameID: String = data.game_id
												val prediction: List[Int] = data.panel_set.map(_.isopen).toList
					              val result: List[Int] = data.panel_set.map(_.iswin).toList
					              val betAmount: Double = data.destination
					              val prize: Double = data.prize.toDouble
												// create OverAllGameHistory object..
												val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
					                                                                      txHash,
					                                                                      gameID,
					                                                                      TH_CODE,
					                                                                      ListOfIntPredictions(accID,
								                                                                                    prediction,
								                                                                                    result,
								                                                                                    betAmount,
								                                                                                    prize),
					                                                                      true,
					                                                                      Instant.now.getEpochSecond)

												overAllHistory.gameAdd(gameHistory)
													.map { _ =>
									          dynamicBroadcast ! Array(gameHistory)
									          dynamicProcessor ! DailyTask(accID, TH_GAME_ID, 1)
														dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, 1)
										        true
										      }
											}
											else Future(false)
										}
									} yield (processedHistory)
      					}
      					.getOrElse(Future(false))
      			}
      			.getOrElse(Future(false))
				}
				else Future(false)
      }
    } yield ((isSaveHistory, processWithdraw.getOrElse(null)))
	}
}