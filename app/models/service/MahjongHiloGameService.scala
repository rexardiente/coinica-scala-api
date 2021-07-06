package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorRef
import play.api.libs.json._
import utils.Config.{ SUPPORTED_SYMBOLS, MJHilo_CODE, MJHilo_GAME_ID }
import models.domain.eosio.MahjongHiloGameData
import models.domain._

@Singleton
class MahjongHiloGameService @Inject()(contract: utils.lib.MahjongHiloEOSIO,
																			userAccountService: UserAccountService,
																			overAllHistory: OverAllHistoryService,
																			@Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
																			@Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
	def declareWinHand(gameID: Int): Future[Boolean] =
		contract.declareWinHand(gameID)

	def declareKong(gameID: Int, sets: Seq[Int]): Future[Boolean] =
		contract.declareKong(gameID, sets)

	def discardTile(gameID: Int, index: Int): Future[Boolean] =
		contract.discardTile(gameID, index)

	def playHilo(accID: UUID, gameID: Int, option: Int): Future[(Int, String, Option[MahjongHiloGameData])] = {
		for {
			onPlay <- contract.playHilo(gameID, option)
			gameData <- getUserData(gameID)
			_ <- Future.successful {
				onPlay
					.map { v =>
						val (isWin, hash): (Boolean, String) = v
						// if lost, get game data..
						if (!isWin) {
							for {
								// check if tx_hash already exists to DB history..
								isExists <- overAllHistory.gameIsExistsByTxHash(hash)
								// return true if successful adding to DB else false
								_ <- Future.successful {
									// check if not exists and has game data...
									if (!isExists && gameData != None) {
										val data: MahjongHiloGameData = gameData.get
										val gameID: String = data.game_id
										val prediction: Int = option
			              val result: Int = data.prediction
			              val betAmount: Double = data.hi_lo_stake.toDouble
			              val prize: Double = 0
										// create OverAllGameHistory object..
										val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
			                                                                      hash,
			                                                                      gameID,
			                                                                      MJHilo_CODE,
			                                                                      IntPredictions(accID,
			                                                                                    prediction,
			                                                                                    result,
			                                                                                    betAmount,
			                                                                                  	prize),
			                                                                      true,
			                                                                      Instant.now.getEpochSecond)

										overAllHistory.gameAdd(gameHistory).map { x =>
							        if(x > 0) {
							          dynamicBroadcast ! Array(gameHistory)
							          dynamicProcessor ! DailyTask(accID, MJHilo_GAME_ID, 1)
												dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, if (prize == 0) 0 else 1)
							        }
							      }
									}

								}
							} yield ()
						}
					}
					.getOrElse(3)
			}

			// if win or lost return (code int and hash string)
			// else return (code int = 3 and hash null)
			(isWin, hash, gameData) <- Future.successful {
				onPlay
					.map { v =>
						val (isWin, hash): (Boolean, String) = v
						if (isWin) (1, hash, gameData)
						else (2, hash, gameData)
					}
					.getOrElse(3, null, None)
			}

		} yield ((isWin, hash, gameData))
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

	def withdraw(id: UUID, gameID: Int): Future[(Int, String)] = {
		for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- contract.getUserData(gameID)
      // get prize amount from smartcontract
      getPrize <- Future.successful(gameData.map(_.hi_lo_balance).getOrElse(BigDecimal(0)))
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
      isSaveHistory <- {
      	if (updateBalance > 0) {
					val txHash: String = processWithdraw._2
					val accID: UUID = hasWallet.map(_.id).getOrElse(UUID.randomUUID)

					for {
						isExists <- overAllHistory.gameIsExistsByTxHash(txHash)
						processedHistory <- {
							if (!isExists && gameData != None) {
								val data: MahjongHiloGameData = gameData.get
								val gameID: String = data.game_id
								val prediction: Int = data.prediction
	              val result: Int = data.prediction
	              val betAmount: Double = data.hi_lo_stake.toDouble
	              val prize: Double = data.hi_lo_balance.toDouble
								// create OverAllGameHistory object..
								val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
	                                                                      txHash,
	                                                                      gameID,
	                                                                      MJHilo_CODE,
	                                                                      IntPredictions(accID,
	                                                                                    prediction,
	                                                                                    result,
	                                                                                    betAmount,
	                                                                                    prize),
	                                                                      true,
	                                                                      Instant.now.getEpochSecond)

								overAllHistory.gameAdd(gameHistory)
									.map { _ =>
					          dynamicBroadcast ! Array(gameHistory)
					          dynamicProcessor ! DailyTask(accID, MJHilo_GAME_ID, 1)
										dynamicProcessor ! ChallengeTracker(accID, betAmount, prize, 1, 1)
						        true
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

	def getUserData(gameID: Int): Future[Option[MahjongHiloGameData]] =
		contract.getUserData(gameID)

}