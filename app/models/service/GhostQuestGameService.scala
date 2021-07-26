package models.service

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.mutable.{ HashMap, ListBuffer }
import akka.actor._
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain.eosio._
import utils.Config.{ SUPPORTED_SYMBOLS, GQ_CODE, GQ_GAME_ID }
import models.domain.eosio.{ GhostQuestCharacter, GhostQuestCharacterHistory }
import models.repo.eosio._
import models.domain.{ OverAllGameHistory, PaymentType }

@Singleton
class GhostQuestGameService @Inject()(contract: utils.lib.GhostQuestEOSIO,
                                      userAccountService: UserAccountService,
                                      overAllHistory: OverAllHistoryService,
                                      battleResult: GhostQuestBattleResultRepo,
                                      ghostQuestCharacterService: GhostQuestCharacterService,
                                      gameHistoryRepo: GhostQuestCharacterGameHistoryRepo,
                                      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
                                      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
  private def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)
  def getUserData(id: Int): Future[Option[GhostQuestGameData]] =
    contract.getUserData(id)
  def getAllCharacters(): Future[Option[Seq[GhostQuestTableGameData]]] =
    contract.getAllCharacters()
  def initialize(id: Int, username: String): Future[Option[String]] =
    contract.initialize(id, username)
  def generateCharacter(id: UUID, username: String, gameID: Int, currency: String, quantity: Int, limit: Int): Future[Int] = {
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
        if (hasEnoughBalance) contract.generateCharacter(gameID, username, quantity, limit)
        else Future(None)
      }
      // deduct balance on the account
      updateBalance <- {
        if (initGame != None) userAccountService.deductBalanceByCurrency(id, currency, currentValue)
        else Future(0)
      }
      // update Characters list for battle and broadcast to UI the newly created character
    } yield (updateBalance)
  }
  def addLife(id: Int, key: String): Future[Option[String]] =
    contract.addLife(id, key)
  def eliminate(id: Int, key: String): Future[Option[String]] =
    contract.eliminate(id, key)
  def withdraw(id: UUID, username: String, gameID: Int, key: String): Future[(Boolean, Option[String])] = {
    for {
      hasWallet <- userAccountService.getUserAccountWallet(id)
      gameData <- getUserData(gameID)
      (character, prize) <- Future.successful {
        gameData
          .map { data =>
            // val characters: Seq[GhostQuestCharacter] = data.characters
            val selectedCharacter: Option[GhostQuestCharacter] = data.characters.filter(_.key == key).headOption
            val prize: BigDecimal = selectedCharacter.map(_.value.prize).getOrElse(BigDecimal(0))
            (selectedCharacter, Some(prize))
          }
          .getOrElse(None, None)
      }
      processWithdraw <- {
        if (prize.getOrElse(BigDecimal(0)) > 0) contract.withdraw(gameID, key)
        else Future(None)
      }
      // if successful, add new balance to account..
      updateBalance <- {
        hasWallet.map { _ =>
          processWithdraw
            .map(_ => userAccountService.addBalanceByCurrency(id, SUPPORTED_SYMBOLS(0).toUpperCase, prize.getOrElse(0)))
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
                        val gameID: String = txHash // use tx_hash as game
                        // val prize: Double = data.prize.toDouble
                        // create OverAllGameHistory object..
                        val gameHistory: OverAllGameHistory = OverAllGameHistory(UUID.randomUUID,
                                                                                txHash,
                                                                                gameID,
                                                                                GQ_CODE,
                                                                                PaymentType(username,
                                                                                            "WITHDRAW",
                                                                                            prize.getOrElse(BigDecimal(0)).toDouble),
                                                                                true,
                                                                                Instant.now.getEpochSecond)

                        overAllHistory.gameAdd(gameHistory)
                          .map { _ =>
                            dynamicBroadcast ! Array(gameHistory)
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
    } yield ((isSaveHistory, processWithdraw))
  }
  def battleResult(gameid: String, winner: (String, Int), loser: (String, Int)): Future[Option[String]] =
    contract.battleResult(gameid, winner, loser)

  def getHistoryByCharacterID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByCharacterID(id)
  def getGQGameHistoryByUserID(id: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByUserID(id)
  def getGameHistoryByGameIDAndCharacterID(id: String, ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByGameIDAndCharacterID(id, ownerID)
  def filteredGameHistoryByID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistoryRepo.filteredGameHistoryByID(id)
  def getAllGameHistory(): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistoryRepo.getAllGameHistory()
  // get all characters from specific player
  // characters are separate by status
  def getAllCharactersDataAndHistoryLogsByUser(id: UUID, ownerID: Int): Future[JsValue] = {
    for {
      // get all characters that are still alive, on smartcontract
      // alive <- ghostQuestCharacterService.getByUser(user)
      alive <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
      // get all characters that are already eliminated and
      // convert to GhostQuestCharacter from GhostQuestCharacterHistory
      eliminated <- ghostQuestCharacterService
          .findGhostQuestCharacterHistoryByOwnerID(ownerID)
          .map(_.map(_.toCharacterData))

      // merge two Future[Seq] in single Future[Seq]
      logs <- {
        val characters: Seq[GhostQuestCharacter] = mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive, eliminated)
        getGameLogsOfCharacters(characters)
      }
    } yield logs
  }

  def getCharactInfoByKey(key: String): Future[JsValue] = {
    for {
      // check if character already eliminate
      eliminated <- ghostQuestCharacterService
          .getGhostQuestCharacterHistoryByKey(key)
          .map(_.map(_.toCharacterData))
      // check if character still in-battle
      smartContractCharacters <- getAllCharacters()
      alive <- Future.successful {
        smartContractCharacters.map { gameTable =>
          val seqGameTable: Seq[GhostQuestTableGameData] = gameTable
          // val aa: Option[Seq[GhostQuestCharacter]] =
          gameTable.map(_.game_data.characters.filter(_.key == key)).headOption
        }
        .getOrElse(None)
      }
      logs <- {
        val characters: Seq[GhostQuestCharacter] = mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive.getOrElse(Seq.empty), eliminated)
        getGameLogsOfCharacters(characters)
      }
    } yield logs
  }
  def getGameLogsOfCharacters(characters: Seq[GhostQuestCharacter]): Future[JsValue] = {
    // iterate each characters games history..
    val tupled: Future[Seq[(JsValue, JsValue)]] =
      Future.sequence(characters.map({ character =>
        val seqHistory: Future[Seq[GhostQuestCharacterGameHistory]] = getHistoryByCharacterID(character.key)

        seqHistory.map(v => (Json.toJson(character), Json.toJson(v)))
      }))
    // convert Seq[JSON] to JsArray
    tupled.map(x => Json.toJson(x))
  }
  // Top 10 results of characters
  def highEarnCharactersAllTime(): Future[Seq[GhostQuestCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getAllGameHistory()
      grouped <- Future.successful(classifyHighEarnCharacter(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (key, (ownerID, amount)) =>
          for {
            ownedCharactersOnSmartContract <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
            alive <- Future.successful(ownedCharactersOnSmartContract.filter(_.key == key))
            eliminated <- ghostQuestCharacterService
              .getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID, key)
              .map(_.map(_.toCharacterData))
          } yield ((mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map {
          case (c, amount) => new GhostQuestCharactersRankByEarned(c.key, c.value.owner_id, c.value.rarity, amount)
        }
      }
    } yield ranks
  }

  def highEarnCharactersDaily(): Future[Seq[GhostQuestCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getGameHistoryByDateRange(Instant.now.getEpochSecond - (24*60*60), Instant.now.getEpochSecond)
      grouped <- Future.successful(classifyHighEarnCharacter(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (key, (ownerID, amount)) =>
          for {
            ownedCharactersOnSmartContract <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
            alive <- Future.successful(ownedCharactersOnSmartContract.filter(_.key == key))
            eliminated <- ghostQuestCharacterService
              .getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID, key)
              .map(_.map(_.toCharacterData))
          } yield ((mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map {
          case (c, amount) => new GhostQuestCharactersRankByEarned(c.key, c.value.owner_id, c.value.rarity, amount)
        }
      }
    } yield ranks
  }

  def highEarnCharactersWeekly(): Future[Seq[GhostQuestCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getGameHistoryByDateRange(Instant.now.getEpochSecond - ((24*60*60) * 7), Instant.now.getEpochSecond)
      grouped <- Future.successful(classifyHighEarnCharacter(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (key, (ownerID, amount)) =>
          for {
            ownedCharactersOnSmartContract <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
            alive <- Future.successful(ownedCharactersOnSmartContract.filter(_.key == key))
            eliminated <- ghostQuestCharacterService
              .getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID, key)
              .map(_.map(_.toCharacterData))
          } yield ((mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map {
          case (c, amount) => new GhostQuestCharactersRankByEarned(c.key, c.value.owner_id, c.value.rarity, amount)
        }
      }
    } yield ranks
  }

  def classifyHighEarnCharacter(history: Seq[GhostQuestCharacterGameHistory], limit: Int): Seq[(String, (Int, Double))] = {
    val characters = HashMap.empty[String, (Int, Double)]
    history.map { tx =>
      // processTxStatus
      if (!characters.exists(_._1 == tx.winnerID)) characters(tx.winnerID) = (tx.winner, 0)
      if (!characters.exists(_._1 == tx.loserID)) characters(tx.loserID) = (tx.loser, 0)

      // process winner character
      val winner = characters(tx.winnerID)
      characters.update(tx.winnerID, (tx.winner, winner._2 + 1))

      val loser = characters(tx.loserID)
      characters.update(tx.loserID, (tx.loser, loser._2 - 1))
    }
    characters.filter(_._2._2 > 0).toSeq.sortBy(- _._2._2).take(limit)
  }

  def getCharacterByUserAndID(ownerID: Int, id: String): Future[JsValue] = {
    for {
      // get all characters that are still alive
      alive <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
      // get all characters that are already eliminated and
      // convert to GhostQuestCharacter from GhostQuestCharacterHistory
      eliminated <- ghostQuestCharacterService
        .getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID, id)
        .map(_.map(_.toCharacterData))
      // merge two Future[Seq] in single Future[Seq]
      logs <- {
        val characters: Seq[GhostQuestCharacter] = mergeSeq[GhostQuestCharacter, Seq[GhostQuestCharacter]](alive, eliminated)
        getGameLogsOfCharacters(characters)
      }
    } yield logs
  }

  def getAliveCharacters(ownerID: Int): Future[JsValue] = {
    for {
      characters <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
      logs <- getGameLogsOfCharacters(characters)
    } yield logs
  }

  def getCharacterDataByID(ownerID: Int, key: String): Future[JsValue] = {
      for {
        characters <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty))
        isExists <- Future.successful(characters.filter(_.key == key).headOption)
        logs <- getGameLogsOfCharacters(isExists.map(Seq(_)).getOrElse(Seq.empty))
      } yield logs
    }

  def getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID: Int, key: String): Future[JsValue] = {
      for {
        characters <- ghostQuestCharacterService.getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID, key)
        // merge two Future[Seq] in single Future[Seq]
        logs <- getGameLogsOfCharacters(characters.map(_.toCharacterData))
      } yield logs
    }

  def getAllEliminatedCharacters(ownerID: Int): Future[JsValue] = {
    for {
      characters <- ghostQuestCharacterService
        .findGhostQuestCharacterHistoryByOwnerID(ownerID)
        .map(_.map(_.toCharacterData))
      logs <- getGameLogsOfCharacters(characters)
    } yield logs
  }

  // TOD: scheduled process (Weekly for Lifetime Win Streak)
  // get overall history in a week, process and save it to WinStreak tbl
  // ghostQuestCharacterService.getGameHistoryByDateRange(from, to)
  def separateHistoryByCharID(seq: Seq[GhostQuestCharacterGameHistory]): HashMap[String, ListBuffer[(String, Boolean, Long)]] = {
    val counter = HashMap.empty[String, ListBuffer[(String, Boolean, Long)]]
    seq.foreach { history =>
      val gameID = history.id
      val winnerID = history.winnerID
      val loserID = history.loserID
      val time = history.timeExecuted

      // process winner
      if (counter.exists(_._1 == winnerID))
        counter.addOne(winnerID -> { counter(winnerID) += ((gameID, true, time)) })
      else
        counter.addOne(winnerID -> ListBuffer((gameID, true, time)))
      // process loser
      if (counter.exists(_._1 == loserID))
        counter.addOne(loserID -> { counter(loserID) += ((gameID, false, time)) })
      else
        counter.addOne(loserID -> ListBuffer((gameID, false, time)))
    }
    // remove has less than 1 amount
    counter // .filter(x => x._2.map(v => if (v._3 > 0) true else false).contains(false))
  }

  def calcWinStreak(characters: HashMap[String, ListBuffer[(String, Boolean, Long)]]): HashMap[String, Int] = {
    characters.map { character =>
      val status       : ListBuffer[(String, Boolean, Long)] = character._2
      val streakCounter: ListBuffer[Int] = ListBuffer.empty[Int]
      val tempList     : ListBuffer[Int] = ListBuffer.empty[Int]

      status.zipWithIndex.map {
        case (v, i) =>
          if (v._2) tempList.addOne(i)
          else {
            val range = status.slice(tempList.headOption.getOrElse(0), tempList.lastOption.getOrElse(0))
            if (!range.isEmpty) {
              streakCounter += range.size
              tempList.clear()
            }
          }
          // check if it is the last itr to finallized the list result
          if (status.last == v && !tempList.isEmpty) {
            streakCounter += tempList.size
            tempList.clear()
          }
      }
      (character._1, streakCounter)
    }
    .map { case (id, list) => (id, list.maxOption.getOrElse(0)) }
  }

  def winStreakPerDay(): Future[List[GhostQuestCharactersRankByWinStreak]] = {
    val today: Long = Instant.now().getEpochSecond
    for {
      history <- gameHistoryRepo.getGameHistoryByDateRange(today - (24*60*60), today)
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }

  def winStreakPerWeekly(): Future[List[GhostQuestCharactersRankByWinStreak]] = {
    val today: Long = Instant.now().getEpochSecond
    for {
      history <- gameHistoryRepo.getGameHistoryByDateRange(today - ((24*60*60) * 7), today)
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }

  def winStreakLifeTime(): Future[List[GhostQuestCharactersRankByWinStreak]] = {
    for {
      history <- gameHistoryRepo.getAllGameHistory()
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }
  // get character info
  // convert iterable to List[object]
  // filter and remove win_streak = 0
  // sort by high to low and take only top 10 results
  def calcStreakToStreakObject(v: HashMap[String, Int]): Future[List[GhostQuestCharactersRankByWinStreak]] = {
      Future.sequence {
        v.map { v =>
          val key: String = v._1
          val ownerID: Int = v._2
          for {
            isAlive <- contract.getUserData(ownerID).map(_.map(_.characters).getOrElse(Seq.empty).filter(_.key == key))
            // al <- Future.successful(characters.filter(_.key == key).headOption)
            isEliminated <- ghostQuestCharacterService.findGhostQuestCharacterHistory(key)
            // either <- Future.successful(al.getOrElse(el.get))
            winstreak <- Future.successful {
              if (!isAlive.isEmpty)
                isAlive.map(info => GhostQuestCharactersRankByWinStreak(key, info.value.owner_id, info.value.rarity, ownerID)).toSeq
              else
                isEliminated.map(info => GhostQuestCharactersRankByWinStreak(key, info.owner_id, info.rarity, ownerID)).toSeq
            }
          } yield (winstreak.headOption.getOrElse(null))
        }.toList
    }
  }
  def insertBattleResult(v: GhostQuestBattleResult): Future[Int] = battleResult.insert(v)
  def removeAllBattleResult(): Future[Int] = battleResult.removeAll()
  def getAllBattleResult(): Future[Seq[GhostQuestBattleResult]] = battleResult.all()
}