package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorRef
import play.api.libs.json._
import utils.SystemConfig._
import models.repo.TaskRepo
import models.domain.eosio.TreasureHuntGameData
import models.domain._

@Singleton
class TreasureHuntGameService @Inject()(contract: utils.lib.TreasureHuntEOSIO,
																				platformConfigService: PlatformConfigService,
																				taskRepo: TaskRepo,
																				userAccountService: UserAccountService,
																				overAllHistory: OverAllHistoryService,
																				@Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
																				@Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
	private val defaultGameName: String = "treasurehunt"
	private def COIN_USDC: PlatformCurrency = SUPPORTED_CURRENCIES.find(_.name == "usd-coin").getOrElse(null)

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
										task <- taskRepo.getTaskWithOffset(0)
										// return true if successful adding to DB else false
										onSaveHistory <- {
											if (!isExists) {
												val gameID: String = data.game_id
												val prediction: List[Int] = data.panel_set.map(_.isopen).toList
					              val result: List[Int] = data.panel_set.map(_.iswin).toList
					              val betAmount: Double = data.destination
					              val prize: Double = 0
												// create OverAllGameHistory object..
												val gameHistory: OverAllGameHistory =
													OverAllGameHistory(UUID.randomUUID,
                                            hash,
                                            gameID,
                                            defaultGame.map(_.name).getOrElse("default_code"),
                                            ListOfIntPredictions(username, prediction, result, betAmount, prize, None),
                                            true,
                                            instantNowUTC().getEpochSecond)

												overAllHistory.addHistory(gameHistory)
													.map { isAdded =>
									          if (isAdded > 0) {
									          	dynamicBroadcast ! Array(gameHistory)
										          dynamicProcessor ! DailyTask(task.get.id, id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
															dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, prize)
															(1)
									          }
									          else (0)
										      }
											}
											else Future.successful(0)
										}
									} yield (onSaveHistory)
								}
								else Future.successful(1) // return 1 if WIN
							}
							.getOrElse(Future.successful(0))
					}
					.getOrElse(Future.successful(0))
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
												task <- taskRepo.getTaskWithOffset(0)
												// return true if successful adding to DB else false
												onSaveHistory <- {
													if (!isExists) {
														val gameID: String = data.game_id
														val prediction: List[Int] = data.panel_set.map(_.isopen).toList
							              val result: List[Int] = data.panel_set.map(_.iswin).toList
							              val betAmount: Double = data.destination
							              val prize: Double = 0
														// create OverAllGameHistory object..
														val gameHistory: OverAllGameHistory =
															OverAllGameHistory(UUID.randomUUID,
                                                hash,
                                                gameID,
                                                defaultGame.map(_.name).getOrElse("default_code"),
                                                ListOfIntPredictions(username, prediction, result, betAmount, prize, None),
                                                true,
                                                instantNowUTC().getEpochSecond)

														overAllHistory.addHistory(gameHistory)
															.map { isAdded =>
											          if (isAdded > 0) {
											          	dynamicBroadcast ! Array(gameHistory)
												          dynamicProcessor ! DailyTask(task.get.id, id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
																	dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, prize)
																	(1)
											          }
											          else (0)
												      }
													}
													else Future.successful(0)
												}
											} yield (onSaveHistory)
										}
										else Future.successful(1) // return 1 if WIN
									}
									.getOrElse(Future.successful(0))
							}
						} yield ((isWinOrLost, updatedGameData))
					}
					.getOrElse((Future.successful(0, None)))
			}
		} yield (onProcess, isOpenTile.getOrElse(null), gameData)
	}
	def quit(gameID: Int, username: String): Future[Option[String]] =
		contract.treasureHuntQuit(gameID, username)
	def initialize(id: UUID, gameID: Int, currency: String, quantity: Int, destination: Int, enemy: Int):
	Future[Option[String]] = {
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
        else Future.successful(None)
      }
      // deduct balance on the account
      updateBalance <- {
      	initGame
      		.map(_ => userAccountService.deductBalanceByCurrency(hasWallet.get, currency, currentValue))
      		.getOrElse(Future.successful(0))
      }
      isConfirmed <- Future.successful { if (updateBalance > 0) initGame else None }
    } yield (isConfirmed)
	}
	def withdraw(id: UUID, username: String, gameID: Int): Future[(Boolean, String)] = {
		for {
      gameData <- contract.treasureHuntGetUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful(gameData.map(_.prize).getOrElse(BigDecimal(0)))
      processWithdraw <- {
      	if (getPrize > 0) contract.treasureHuntWithdraw(gameID)
      	else Future.successful(None)
      }
      hasWallet <- userAccountService.getUserAccountWallet(id)
      // if successful, add new balance to account..
      updateBalance <- {
      	processWithdraw
    			.map(_ => userAccountService.addBalanceByCurrency(hasWallet.get, COIN_USDC.symbol, getPrize))
    			.getOrElse(Future.successful(0))
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
										task <- taskRepo.getTaskWithOffset(0)
										processedHistory <- {
											if (!isExists) {
												val gameID: String = data.game_id
												val prediction: List[Int] = data.panel_set.map(_.isopen).toList
					              val result: List[Int] = data.panel_set.map(_.iswin).toList
					              val betAmount: Double = data.destination
					              val prize: Double = data.prize.toDouble
												// create OverAllGameHistory object..
												val gameHistory: OverAllGameHistory =
													OverAllGameHistory(UUID.randomUUID,
                                            txHash,
                                            gameID,
                                            defaultGame.map(_.name).getOrElse("default_code"),
                                            ListOfIntPredictions(username, prediction, result, betAmount, prize, None),
                                            true,
                                            instantNowUTC().getEpochSecond)

												overAllHistory.addHistory(gameHistory)
													.map { _ =>
									          dynamicBroadcast ! Array(gameHistory)
									          dynamicProcessor ! DailyTask(task.get.id, id, defaultGame.map(_.id).getOrElse(UUID.randomUUID), 1)
														dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, prize)
										        true
										      }
											}
											else Future.successful(false)
										}
									} yield (processedHistory)
      					}
      					.getOrElse(Future.successful(false))
      			}
      			.getOrElse(Future.successful(false))
				}
				else Future.successful(false)
      }
    } yield ((isSaveHistory, processWithdraw.getOrElse(null)))
	}
}