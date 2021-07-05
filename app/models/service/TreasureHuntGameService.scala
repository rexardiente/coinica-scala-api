package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor._
import play.api.libs.json._
import utils.Config.{ SUPPORTED_SYMBOLS, TH_CODE, TH_GAME_ID }
import models.domain.eosio.TreasureHuntGameData
import models.domain._
// import models.repo.eosio._

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
	def openTile(accID: UUID, gameID: Int, username: String, index: Int): Future[(Boolean, String)] = {
		for {
			openTile <- contract.treasureHuntOpenTile(gameID, username, index)
			// if lost then save to DB, else do nothing
			onLoseProcess <- {
				openTile.map { tile =>
					if (!tile._1) {
						val txHash: String = tile._2

						for {
							isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
							gameData <- userData(gameID)
							processedHistory <- {
								if (!isExists && gameData != None) {
									val data: TreasureHuntGameData = gameData.getOrElse(null)
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

									overAllHistory.gameAdd(gameHistory).map { x =>
						        if(x > 0) {
						          dynamicBroadcast ! Array(gameHistory)
						          dynamicProcessor ! DailyTask(accID, TH_GAME_ID, 1)
											dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, if (prize == 0) 0 else 1)
											true
						        }
						        else false
						      }
								}
								else Future(false)
							}
						} yield (processedHistory)
					}
					else Future(false)

				}
				.getOrElse(Future(false))
			}
			// isWin Process
			isWin <- Future {
				openTile.map { tile =>
					if (tile._1) true
					// if game lost and lose process is successful
					else if (!tile._1 && onLoseProcess) true
					// else game lost but failed to process history...
					else false
				}
				.getOrElse(false)
			}
		} yield ((isWin, openTile.map(_._2).getOrElse(null)))
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
	def withdraw(id: UUID, gameID: Int): Future[(Int, String)] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- contract.treasureHuntGetUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful(gameData.map(_.prize).getOrElse(BigDecimal(0)))
      processWithdraw <- {
      	if (getPrize > 0) contract.treasureHuntWithdraw(gameID)
      	else Future((false, null))
      }
      // if successful, add new balance to account..
      updateBalance <- {
      	hasWallet.map { _ =>
      		if (processWithdraw._1) userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, getPrize)
      		else Future(0)
      	}
      	.getOrElse(Future(0))
      }
      isSaveHistory <- {
      	if (updateBalance > 0) {
					val txHash: String = processWithdraw._2
					val accID: UUID = hasWallet.map(_.id).getOrElse(UUID.randomUUID)

					for {
						isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
						processedHistory <- {
							if (!isExists && gameData != None) {
								val data: TreasureHuntGameData = gameData.getOrElse(null)
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

								overAllHistory.gameAdd(gameHistory).map { x =>
					        if(x > 0) {
					          dynamicBroadcast ! Array(gameHistory)
					          dynamicProcessor ! DailyTask(accID, TH_GAME_ID, 1)
										dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, if (prize == 0) 0 else 1)
										true
					        }
					        else false
					      }
							}
							else Future(false)
						}
					} yield (processedHistory)
				}
				else Future(false)
      }
    } yield ((if (isSaveHistory) 1 else 0, processWithdraw._2))
	}
}