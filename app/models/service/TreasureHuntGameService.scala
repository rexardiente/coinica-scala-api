package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorRef
import play.api.libs.json._
import utils.SystemConfig.COIN_USDC
import models.domain.eosio.TreasureHuntGameData
import models.domain._

@Singleton
class TreasureHuntGameService @Inject()(contract: utils.lib.TreasureHuntEOSIO,
																				platformConfigService: PlatformConfigService,
																				userAccountService: UserAccountService,
																				overAllHistory: OverAllHistoryService,
																				@Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
																				@Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
	private val defaultGameName: String = "treasurehunt"

	def userData(gameID: Int): Future[Option[TreasureHuntGameData]] =
		contract.treasureHuntGetUserData(gameID)
	def autoPlay(id: UUID, gameID: Int, username: String, sets: Seq[Int]): Future[(Int, String, Option[TreasureHuntGameData])] = {
		for {
			// return None if failed tx, Option[(Boolean, String)] if success:
			// true = win and false = lose
			isOpenTiles <- contract.treasureHuntAutoPlay(gameID, username, sets)
			gameData <- userData(gameID)
			// if lost then save to DB, else do nothing
			onProcess <- {
				isOpenTiles
					.map { hash =>
						// if process succesful then proceed checking if win or lose
						gameData
							.map { data =>
								// check if the status is DONE, then save to DB
								if (data.status == 2) {
									for {
										// check if tx_hash already exists to DB history
										isExists <- overAllHistory.gameIsExistsByTxHash(hash)
										defaultGame <- platformConfigService.getGameInfoByName(defaultGameName)
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
					                                                                      defaultGame.map(_.code).getOrElse("default_code"),
					                                                                      ListOfIntPredictions(username,
								                                                                                    prediction,
								                                                                                    result,
								                                                                                    betAmount,
								                                                                                    prize,
								                                                                                  	None),
					                                                                      true,
					                                                                      Instant.now.getEpochSecond)

												overAllHistory.addHistory(gameHistory)
													.map { isAdded =>
									          if (isAdded > 0) {
									          	dynamicBroadcast ! Array(gameHistory)
										          dynamicProcessor ! DailyTask(id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
															dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, 0)
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
		} yield (onProcess, isOpenTiles.getOrElse(null), gameData)
	}
	def openTile(id: UUID, gameID: Int, username: String, index: Int): Future[(Int, String, Option[TreasureHuntGameData])] = {
		for {
			// return None if failed tx, Option[(Boolean, String)] if success:
			// true = win and false = lose
			isOpenTile <- contract.treasureHuntOpenTile(gameID, username, index)
			// if lost then save to DB, else do nothing
			(onProcess, gameData) <- {
				isOpenTile
					.map { hash =>
						// add small delay for tx to reflect on the network
						Thread.sleep(1000)
						for {
							updatedGameData <- userData(gameID)
							isWinOrLost <- {
								// if process succesful then proceed checking if win or lose
								updatedGameData
									.map { data =>
										// check if the status is DONE, then save to DB
										if (data.status == 2) {
											for {
												// check if tx_hash already exists to DB history
												isExists <- overAllHistory.gameIsExistsByTxHash(hash)
												defaultGame <- platformConfigService.getGameInfoByName(defaultGameName)
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
							                                                                      defaultGame.map(_.code).getOrElse("default_code"),
							                                                                      ListOfIntPredictions(username,
										                                                                                    prediction,
										                                                                                    result,
										                                                                                    betAmount,
										                                                                                    prize,
										                                                                                  	None),
							                                                                      true,
							                                                                      Instant.now.getEpochSecond)

														overAllHistory.addHistory(gameHistory)
															.map { isAdded =>
											          if (isAdded > 0) {
											          	dynamicBroadcast ! Array(gameHistory)
												          dynamicProcessor ! DailyTask(id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
																	dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, 0)
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
						} yield ((isWinOrLost, updatedGameData))
					}
					.getOrElse((Future(3, None)))
			}
		} yield (onProcess, isOpenTile.getOrElse(null), gameData)
	}
	def quit(gameID: Int, username: String): Future[Option[String]] =
		contract.treasureHuntQuit(gameID, username)
	def initialize(id: UUID, gameID: Int, currency: String, quantity: Int, destination: Int, enemy: Int): Future[Option[String]] = {
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
        if (hasEnoughBalance) contract.treasureHuntInitialize(gameID, quantity, destination, enemy)
        else Future(None)
      }
      // deduct balance on the account
      updateBalance <- {
      	initGame
      		.map(_ => userAccountService.deductBalanceByCurrency(id, currency, currentValue))
      		.getOrElse(Future(0))
      }
      isConfirmed <- Future { if (updateBalance > 0) initGame else None }
    } yield (isConfirmed)
	}
	def withdraw(id: UUID, username: String, gameID: Int): Future[(Boolean, String)] = {
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
      			.map(_ => userAccountService.addBalanceByCurrency(id, COIN_USDC.symbol.toUpperCase, getPrize))
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
      						val id: UUID = hasWallet.map(_.id).getOrElse(UUID.randomUUID)
      						for {
										isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
										defaultGame <- platformConfigService.getGameInfoByName(defaultGameName)
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
					                                                                      defaultGame.map(_.code).getOrElse("default_code"),
					                                                                      ListOfIntPredictions(username,
								                                                                                    prediction,
								                                                                                    result,
								                                                                                    betAmount,
								                                                                                    prize,
								                                                                                  	None),
					                                                                      true,
					                                                                      Instant.now.getEpochSecond)

												overAllHistory.addHistory(gameHistory)
													.map { _ =>
									          dynamicBroadcast ! Array(gameHistory)
									          dynamicProcessor ! DailyTask(id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
														dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, 1)
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