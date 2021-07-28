package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorRef
import play.api.libs.json._
import utils.Config.{ SUPPORTED_SYMBOLS, MJHilo_CODE, MJHilo_GAME_ID }
import models.domain._
import models.domain.eosio.{ MahjongHiloGameData, MahjongHiloHistory }
import models.repo.eosio.MahjongHiloHistoryRepo

@Singleton
class MahjongHiloGameService @Inject()(contract: utils.lib.MahjongHiloEOSIO,
																			historyRepo: MahjongHiloHistoryRepo,
																			userAccountService: UserAccountService,
																			overAllHistory: OverAllHistoryService,
																			@Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
																			@Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
	def declareWinHand(userGameID: Int): Future[Boolean] =
		contract.declareWinHand(userGameID)
	def resetBet(userGameID: Int): Future[Boolean] =
		contract.resetBet(userGameID)
	def declareKong(userGameID: Int, sets: Seq[Int]): Future[Boolean] =
		contract.declareKong(userGameID, sets)
	def discardTile(userGameID: Int, index: Int): Future[Boolean] =
		contract.discardTile(userGameID, index)
	def playHilo(id: UUID, username: String, userGameID: Int, option: Int): Future[(Int, String, Option[MahjongHiloGameData])] = {
		for {
			onPlay <- contract.playHilo(userGameID, option)
			gameData <- getUserData(userGameID)
			onProcess <- {
				onPlay
					.map { hash =>
						gameData
							.map { data =>
								val gameID: String = data.game_id
								val prediction: Int = data.prediction
	              val result: Int = data.hi_lo_outcome
	              val betAmount: Double = data.hi_lo_stake.toDouble
	              val prize: Double = 0
	              val predictionTiles: JsValue = Json.obj("current_tile" -> data.current_tile,
              																					"standard_tile" -> data.standard_tile)

								for {
									// fetch table history if exists..
									hasHistory <- historyRepo.findByUserGameIDAndGameID(userGameID, gameID)
									predictionProcess <- Future.successful {
										val newPredictions = (prediction, result, data.current_tile, data.standard_tile)
										hasHistory
											.map(v => v.copy(predictions = (v.predictions :+ newPredictions)))
											.getOrElse(null)
									}
									// update table history..
									isUpdated <- {
										if (predictionProcess != null) historyRepo.update(predictionProcess)
										else Future(0)
									}
									isAdded <- {
										if (isUpdated > 0) {
											// if process succesful then proceed checking if win or lose
											if (data.hi_lo_result == 3) {
												for {
													// check if tx_hash already exists to DB history..
													isExists <- overAllHistory.gameIsExistsByTxHash(hash)
													// return true if successful adding to DB else false
													onSaveHistory <- {
														if (!isExists) {
															// create OverAllGameHistory object..
															val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
								                                                                      hash,
								                                                                      gameID,
								                                                                      MJHilo_CODE,
								                                                                      IntPredictions(username,
								                                                                                    prediction,
								                                                                                    result,
								                                                                                    betAmount,
								                                                                                  	prize,
								                                                                                  	Some(predictionTiles)),
								                                                                      true,
								                                                                      Instant.now.getEpochSecond)

															overAllHistory
																.gameAdd(gameHistory)
																.map { x =>
													        if(x > 0) {
													          dynamicBroadcast ! Array(gameHistory)
													          dynamicProcessor ! DailyTask(id, MJHilo_GAME_ID, 1)
																		dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, 0)
																		(2)
													        }
													      	else (3)
												      	}
														}
														else Future(3)
													}
												} yield (onSaveHistory)
											} else Future(1)
										} else Future(3)
									}
								} yield (isAdded)
							}
							.getOrElse(Future(3))
					}
					.getOrElse(Future(3))
			}
		} yield ((onProcess, onPlay.getOrElse(null), gameData))
	}
	// initialize game data and mahjong game table
	def initialize(userGameID: Int): Future[Int] = {
		for {
			isInitialized <- contract.initialize(userGameID)
			gameData <- getUserData(userGameID)
			processHistory <- Future.successful {
				if (isInitialized) gameData.map(v => MahjongHiloHistory(v.game_id, userGameID))
				else None
			}
			// add into the DB history
			isAdded <- processHistory.map(historyRepo.insert(_)).getOrElse(Future(0))
		} yield (isAdded)
	}
	def reset(userGameID: Int): Future[Int] = {
		for {
			currentGameData <- getUserData(userGameID)
			hasHistory <- {
				currentGameData
					.map(v => historyRepo.findByUserGameIDAndGameID(userGameID, v.game_id))
					.getOrElse(Future(None))
			}
			// update DB history gamedata
			isUpdated <- {
				if (hasHistory != None) {
					val updatedHistory: MahjongHiloHistory = hasHistory.get.copy(gameData = currentGameData, status = true)
					historyRepo.update(updatedHistory)
				}
				// if not found then add to DB
				else {
					currentGameData
						.map { v =>
							val newHistory: MahjongHiloHistory = new MahjongHiloHistory(v.game_id, userGameID, Seq.empty, currentGameData, true)
							historyRepo.insert(newHistory)
						}
						.getOrElse(Future(0))
				}
			}
			// check if DB has been update and proceed to reseting the gamedata
			isReseted <- if (isUpdated > 0) contract.reset(userGameID) else Future(false)
			newGameData <- getUserData(userGameID)
			process <- {
				if (isReseted)
					newGameData
						.map(v => historyRepo.insert(MahjongHiloHistory(v.game_id, userGameID)))
						.getOrElse(Future(0))
				else Future(0)
			}
		} yield (process)
	}
	def quit(userGameID: Int): Future[Int] = {
		for {
			isGameEnded <- contract.quit(userGameID)
			gameData <- getUserData(userGameID)
			processHistory <- Future.successful {
				if (isGameEnded) gameData.map(v => MahjongHiloHistory(v.game_id, userGameID))
				else None
			}
			// add into the DB history
			isAdded <- processHistory.map(historyRepo.insert(_)).getOrElse(Future(0))
		} yield (isAdded)
	}
	def addBet(id: UUID, userGameID: Int, currency: String, quantity: Int): Future[Int] = {
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
        if (hasEnoughBalance) contract.addBet(userGameID, quantity)
        else Future(false)
      }
      // deduct balance on the account
      updateBalance <- {
        if (isAdded) userAccountService.deductBalanceByCurrency(id, currency, currentValue)
        else Future(0)
      }
    } yield (updateBalance)
	}

	def start(userGameID: Int): Future[Boolean] =
		contract.start(userGameID)
	def transfer(userGameID: Int): Future[Boolean] =
		contract.transfer(userGameID)
	def withdraw(id: UUID, username: String, userGameID: Int): Future[(Boolean, String)] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- getUserData(userGameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful(gameData.map(_.hi_lo_balance).getOrElse(BigDecimal(0)))
      // save first to history before removing to smartcontract...
      processWithdraw <- {
      	if (getPrize > 0) contract.withdraw(userGameID)
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
	      					val id: UUID = hasWallet.map(_.id).getOrElse(UUID.randomUUID)
									for {
										isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
										processedHistory <- {
											if (!isExists) {
												val gameID: String = data.game_id
												val prediction: Int = data.prediction
					              val result: Int = data.prediction
					              val betAmount: Double = data.hi_lo_stake.toDouble
					              val prize: Double = data.hi_lo_balance.toDouble
					              val predictionTiles: JsValue = Json.obj("current_tile" -> data.current_tile,
				              																					"standard_tile" -> data.standard_tile)
												// create OverAllGameHistory object..
												val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
					                                                                      txHash,
					                                                                      gameID,
					                                                                      MJHilo_CODE,
					                                                                      IntPredictions(username,
					                                                                                    prediction,
					                                                                                    result,
					                                                                                    betAmount,
					                                                                                    prize,
					                                                                                  	Some(predictionTiles)),
					                                                                      true,
					                                                                      Instant.now.getEpochSecond)

												overAllHistory.gameAdd(gameHistory)
													.map { _ =>
									          dynamicBroadcast ! Array(gameHistory)
									          dynamicProcessor ! DailyTask(id, MJHilo_GAME_ID, 1)
														dynamicProcessor ! ChallengeTracker(id, betAmount, prize, 1, 1)
										        true
										      }
											}
											else Future(false)
										}
									} yield (processedHistory)
	      				}.getOrElse(Future(false))
      			}
      			.getOrElse(Future(false))
				}
				else Future(false)
      }
    } yield ((isSaveHistory, processWithdraw.getOrElse(null)))
	}
	def getUserData(userGameID: Int): Future[Option[MahjongHiloGameData]] =
		contract.getUserData(userGameID)
	def getHiLoWinRate() = ???
	def getMaxPayout() = ???
	def getConsecutiveHilo() = ???
	def getTotalPlayed() = ???
	def getWinRate() = ???
	def getTotalWin() = ???
	def getAvgWinScore() = ???
	def getAvgWinRound() = ???
	def getShortestWinRound() = ???
	def getMonthlyRanking() = ???

}