package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import akka.actor.ActorRef
import play.api.libs.json._
import Ordering.Double.IeeeOrdering
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
	def declareWinHand(userGameID: Int): Future[Option[String]] =
		contract.declareWinHand(userGameID)
	def declareKong(userGameID: Int, sets: Seq[Int]): Future[Option[String]] =
		contract.declareKong(userGameID, sets)
	def discardTile(userGameID: Int, index: Int): Future[Option[String]] =
		contract.discardTile(userGameID, index)
	def playHilo(id: UUID, username: String, userGameID: Int, option: Int): Future[(Int, String, Option[MahjongHiloGameData])] = {
		for {
			currentGameData <- getUserData(userGameID)
			// hasHistory <- {
			// 	currentGameData
			// 		.map(x => historyRepo.findByUserGameIDAndGameID(x.game_id, userGameID))
			// 		.getOrElse(Future.successful(None))
			// }

			// Oct 22, 2021:
			// Due to failure of inserting the game into DB but initialized in smartcontract,
			// its safer for checking directly on smartcontract tbl..
			onPlay <- currentGameData.map(_ => contract.playHilo(userGameID, option)).getOrElse(Future.successful(None))
			// hasHistory.map(_ => contract.playHilo(userGameID, option)).getOrElse(Future.successful(None))
			updatedGamData <- getUserData(userGameID)
			onProcess <- {
				onPlay
					.map { hash =>
						updatedGamData
							.map { data =>
								val gameID: String = data.game_id
								val prediction: Int = data.prediction
	              val result: Int = data.hi_lo_outcome
	              val betAmount: Double = data.hi_lo_bet.toDouble
	              val prize: Double = 0
	              val predictionTiles: JsValue = Json.obj("current_tile" -> data.current_tile,
              																					"standard_tile" -> data.standard_tile)

								for {
									// fetch table history if exists..
									updatedHistory <- historyRepo.findByUserGameIDAndGameID(gameID, userGameID)
									// update table history..
									isUpdated <- {
										updatedHistory
											.map { history =>
												val newPredictions = (prediction, result, data.current_tile, data.standard_tile)
												historyRepo.update(history.copy(predictions = (history.predictions :+ newPredictions)))
											}
											.getOrElse(Future.successful(0))
									}
									// process for overall history...
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
														else Future.successful(3)
													}
												} yield (onSaveHistory)
											}
											else Future.successful(1)
										}
										else Future.successful(3)
									}
								} yield (isAdded)
							}
							.getOrElse(Future.successful(3))
					}
					.getOrElse(Future.successful(3))
			}
		} yield ((onProcess, onPlay.getOrElse(null), updatedGamData))
	}
	// initialize game data and mahjong game table
	def initialize(userGameID: Int): Future[Int] = {
		for {
			isInitialized <- contract.initialize(userGameID)
			gameData <- getUserData(userGameID)
			// if success init then add to DB history
			insertDB <- {
				isInitialized
					.map { _ =>
						gameData
							.map(v => historyRepo.insert(MahjongHiloHistory(v.game_id, userGameID)))
							.getOrElse(Future.successful(0))
					}.getOrElse(Future.successful(0))
			}
			// if db insertion success, else remove from smartcontract..
			processValidated <- {
				isInitialized
					.map { _ =>
						if (insertDB > 0) Future.successful(insertDB)
						// remove from smartcontract but still return error code..
						else contract.end(userGameID).map(_ => 0)
					}
					.getOrElse(Future.successful(0))
			}
		} yield (processValidated)
	}
	def resetBet(userGameID: Int): Future[Int] = contract.resetBet(userGameID).map(_.map( _ => 1).getOrElse(0))
	// def resetBet(userGameID: Int): Future[Int] = {
	// 	for {
	// 		isReseted <- contract.resetBet(userGameID)
	// 		// get updated user gamedata
	// 		newGameData <- getUserData(userGameID)
	// 		// every game reset insert new gamedata into DB..
	// 		process <- {
	// 			isReseted
	// 				.map { _ =>
	// 					newGameData
	// 						.map(v => historyRepo.insert(MahjongHiloHistory(v.game_id, userGameID)))
	// 						.getOrElse(Future.successful(0))
	// 				}.getOrElse(Future.successful(0))
	// 		}
	// 	} yield (process)
	// }
	def end(userGameID: Int): Future[Int] = {
		for {
			// if has existing game and wants to reset the game..
			// update the existing game to done...
			gameData <- getUserData(userGameID)
			hasHistory <- {
				gameData
					.map(v => historyRepo.findByUserGameIDAndGameID(v.game_id, userGameID))
					.getOrElse(Future.successful(None))
			}
			// update DB history gamedata
			// if predictions is empty remove it from DB else insert
			isUpdated <- {
				hasHistory
					.map { history =>
						val updatedHistory: MahjongHiloHistory = history.copy(gameData = gameData, status = true)

						if (history.predictions.isEmpty) historyRepo.delete(history.gameID)
						else historyRepo.update(updatedHistory)
					}
					.getOrElse(Future.successful(0))
			}
			process <- {
				// if happens data is not inserted to DB but game is initialized
				// still able to remove the game from smartcontract
				// if (existingGameData != None && hasHistory == None)
				// 	contract.end(userGameID).map(_.map(_ => 1).getOrElse(0))
				// else if (existingGameData != None && hasHistory != None && isUpdated > 0)
				if (isUpdated > 0) contract.end(userGameID).map(_.map(_ => 1).getOrElse(0))
				else Future.successful(0)
			}
			// if smartcontract updated failed then redo the update..
			_ <- Future.successful {
				hasHistory
					.map { history =>
						val updatedHistory: MahjongHiloHistory = history.copy(status = false)
						historyRepo.update(updatedHistory)
					}
			}
		} yield (process)
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
        else Future.successful(None)
      }
      // deduct balance on the account
      updateBalance <- {
      	isAdded
      		.map(_ => userAccountService.deductBalanceByCurrency(id, currency, currentValue))
      		.getOrElse(Future.successful(0))
      }
    } yield (updateBalance)
	}

	def start(userGameID: Int): Future[Option[String]] =
		contract.start(userGameID)
	def transfer(userGameID: Int): Future[Option[String]] =
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
      	else Future.successful(None)
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	hasWallet.map { _ =>
      		processWithdraw
      			.map(_ => userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize))
      			.getOrElse(Future.successful(0))
      	}
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
											else Future.successful(false)
										}
									} yield (processedHistory)
	      				}.getOrElse(Future.successful(false))
      			}
      			.getOrElse(Future.successful(false))
				}
				else Future.successful(false)
      }
    } yield ((isSaveHistory, processWithdraw.getOrElse(null)))
	}
	def getUserData(userGameID: Int): Future[Option[MahjongHiloGameData]] =
		contract.getUserData(userGameID)
	def getUserGameHistory(userGameID: Int): Future[Seq[MahjongHiloHistory]] =
		historyRepo.getByUserGameID(userGameID)
	def getMaxPayout(userGameID: Int): Future[Double] = {
		for {
			// get all history and extract game ID
			seqOfGameIDs <- historyRepo.getByUserGameID(userGameID).map(_.map(_.gameID))
			// fetch overall history data using seqOfGameIDs
			histories <- overAllHistory.getOverallGameHistoryByGameID(seqOfGameIDs)
			seqAmount <- Future.successful(histories.map(_.info.amount))
		} yield (if (seqAmount.isEmpty) 0D else seqAmount.max)
	}
	def getMaxConsecutiveHilo(userGameID: Int): Future[Int] = {
		for {
			histories <- historyRepo.getByUserGameID(userGameID)
			process <- Future.successful {
				histories.map { history =>
					// prediction, result, currentTile, standardTile
					val gameResults: List[Boolean] = history.predictions.map(x => x._1 == x._2).toList
					// grouping consecutive identical elements
					val split: List[List[Boolean]] = splitConsecutiveValue(gameResults)
					// remove consecutive loses from the list..
					split.filterNot(_.contains(false)).map(_.size).maxOption.getOrElse(0)
				}
			}
		} yield (if (process.isEmpty) 0 else process.max)
	}
	def getTotalPlayed(userGameID: Int): Future[Int] =
		historyRepo
			.getByUserGameID(userGameID)
			.map(_.map(_.predictions.size).sum)
	def getHiloTotalWin(userGameID: Int): Future[Int] = {
		for {
			histories <- historyRepo.getByUserGameID(userGameID)
			process <- Future.successful {
				histories.map { history =>
					// prediction, result, currentTile, standardTile
					val gameResults: List[Boolean] = history.predictions.map(x => x._1 == x._2).toList
					gameResults.filter(_ == true).size
				}
			}
		} yield (if (process.isEmpty) 0 else process.max)
	}
	def getHiloAvgWinScore(userGameID: Int) = ???
	def getHiloAvgWinRound(userGameID: Int) = ???
	def getShortestWinRound(userGameID: Int) = ???
	// top 10 players on the month based on earnings
	def getMonthlyRanking(): Future[Seq[JsValue]] = {
		for {
			histories <- historyRepo.all()
			// group history by user
			// scala.collection.immutable.Map[Int,Seq[models.domain.eosio.MahjongHiloHistory]]
			groupedByUser <- Future.successful(histories.groupBy(_.userGameID))
			// returns userGameID, totalPayout and WinRate
			getPayoutAndWinRate <- Future.sequence {
				groupedByUser
					.map { case (id, histories) =>
						for {
							totalPayout <- calculateTotalPayoutOnSeqOfHistory(histories)
							winRate <- Future.successful(calculateWinRateOnSeqOfHistory(histories))
						} yield (id, totalPayout, winRate)
					}
					.toSeq
			}
			// remove 0 total payout, sort by totalpayout and take only 10 results
			sortedRanking <- Future.successful(getPayoutAndWinRate.filter(_._2 > 0).sortBy(_._2).take(10))
			// process sorted results and get account details
			processed <- Future.sequence {
				sortedRanking.map { case (id, totalPayout, winRate) =>
					for {
						hasAccount <- userAccountService.getAccountByGameID(id)
						json <- Future.successful {
							hasAccount
								.map { acc =>
									Json.obj("username" -> acc.username,
													"total_payout" -> totalPayout,
													"win_rate" -> winRate)
								}
								.getOrElse(JsNull)
						}
					} yield (json)
				}
				// remove JsNull just in case account not found..
				.filter(_ != Future.successful(JsNull))
			}
		} yield (processed)
	}
	def calculateTotalPayoutOnSeqOfHistory(v: Seq[MahjongHiloHistory]): Future[Double] = {
		for {
			seqOfGameIDs <- Future.successful(v.map(_.gameID))
			histories <- overAllHistory.getOverallGameHistoryByGameID(seqOfGameIDs)
			seqAmount <- Future.successful(histories.map(_.info.amount))
		} yield (seqAmount.sum)
	}
	def calculateWinRateOnSeqOfHistory(v: Seq[MahjongHiloHistory]): Double = {
		try {
			val processedGameResult: Seq[(Int, Int)] =
				v.map { history =>
					val gameResults: List[Boolean] = history.predictions.map(x => x._1 == x._2).toList
					val totalWin: List[Boolean] = gameResults.filter(_ == true)
					// (totalWin.size / gameResults.size) * 100
					(gameResults.size, totalWin.size)
				}
			val totalGamesPlayed: Int = processedGameResult.map(_._1).sum
			val totalWins: Int = processedGameResult.map(_._2).sum
			((totalWins / totalGamesPlayed).toDouble * 100)
		} catch {
			case e : Throwable => 0D
		}
	}
	def getHiLoWinRate(userGameID: Int): Future[Double] = {
		for {
			histories <- historyRepo.getByUserGameID(userGameID)
			winRate <- Future.successful(calculateWinRateOnSeqOfHistory(histories))
		} yield (winRate)
	}
	def splitConsecutiveValue[T](list: List[T]) : List[List[T]] = list match {
	  case h::t =>
	  	val segment = list takeWhile {h ==}
	    segment :: splitConsecutiveValue(list drop segment.length)
	  case Nil => Nil
	}
}