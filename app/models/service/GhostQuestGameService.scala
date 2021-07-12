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
import models.domain.eosio.GQ.v2.{ GQCharacterData, GQCharacterDataHistory }
import models.repo.eosio._
import models.domain.{ OverAllGameHistory, PaymentType }

@Singleton
class GhostQuestGameService @Inject()(contract: utils.lib.GhostQuestEOSIO,
                                      userAccountService: UserAccountService,
                                      overAllHistory: OverAllHistoryService,
                                      charDataRepo: GQCharacterDataRepo,
                                      gameHistoryRepo: GQCharacterGameHistoryRepo,
                                      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
                                      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef) {
  private def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)
  def getUserData(id: Int): Future[Option[GhostQuestGameData]] =
    contract.getUserData(id)
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
            // val characters: Seq[GhostQuestCharacters] = data.characters
            val selectedCharacter: Option[GhostQuestCharacters] = data.characters.filter(_.key == key).headOption
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

  def getHistoryByCharacterID(id: String): Future[Seq[GQCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByCharacterID(id)
  def getGQGameHistoryByUserID(id: UUID): Future[Seq[GQCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByUserID(id)
  def getGameHistoryByUsernameAndCharacterID(player: UUID, id: String): Future[Seq[GQCharacterGameHistory]] =
    gameHistoryRepo.getGameHistoryByUsernameAndCharacterID(player, id)
  def filteredGameHistoryByID(id: String): Future[Seq[GQCharacterGameHistory]] =
    gameHistoryRepo.filteredGameHistoryByID(id)
  def getAllGameHistory(): Future[Seq[GQCharacterGameHistory]] =
    gameHistoryRepo.getAllGameHistory()

  // get all characters from specific player
  // characters are separate by status
  def getAllCharactersDataAndHistoryLogsByUser(user: UUID): Future[JsValue] = {
    for {
      // get all characters that are still alive
      alive <- charDataRepo.getByUser(user)
      // get all characters that are already eliminated and
      // convert to GQCharacterData from GQCharacterDataHistory
      eliminated <- charDataRepo
        .getHistoryByUser(user)
        .map(_.map(GQCharacterDataHistory.toCharacterData))

      // merge two Future[Seq] in single Future[Seq]
      logs <- {
        val characters: Seq[GQCharacterData] = mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)

        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                    v.id,
                    List(GQGameStatus(v.winner, v.winnerID, true),
                        GQGameStatus(v.loser, v.loserID, false)),
                    v.logs,
                    v.timeExecuted)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }

  // Top 10 results of characters
  def highEarnCharactersAllTime(): Future[Seq[GQCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getAllGameHistory()
      grouped <- Future.successful(classifyHighEarnChar(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (id, (player, amount)) =>
          for {
            alive <- charDataRepo.getByUserAndID(player, id)
            eliminated <- charDataRepo
              .getCharacterHistoryByUserAndID(player, id)
              .map(_.map(GQCharacterDataHistory.toCharacterData))
          } yield ((mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map{ case (c, amount) => GQCharactersRankByEarned(c.key, c.owner, c.level, c.`class`, amount) }
      }
    } yield ranks
  }

  def highEarnCharactersDaily(): Future[Seq[GQCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getGameHistoryByDateRange(Instant.now.getEpochSecond - (24*60*60), Instant.now.getEpochSecond)
      grouped <- Future.successful(classifyHighEarnChar(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (id, (player, amount)) =>
          for {
            alive <- charDataRepo.getByUserAndID(player, id)
            eliminated <- charDataRepo
              .getCharacterHistoryByUserAndID(player, id)
              .map(_.map(GQCharacterDataHistory.toCharacterData))
          } yield ((mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map{ case (c, amount) => GQCharactersRankByEarned(c.key, c.owner, c.`class`, c.level, amount) }
      }
    } yield ranks
  }

  def highEarnCharactersWeekly(): Future[Seq[GQCharactersRankByEarned]] = {
    for {
      txs <- gameHistoryRepo.getGameHistoryByDateRange(Instant.now.getEpochSecond - ((24*60*60) * 7), Instant.now.getEpochSecond)
      grouped <- Future.successful(classifyHighEarnChar(txs, 10))
      getCharaterInfo <- Future.sequence {
        grouped.map { case (id, (player, amount)) =>
          for {
            alive <- charDataRepo.getByUserAndID(player, id)
            eliminated <- charDataRepo
              .getCharacterHistoryByUserAndID(player, id)
              .map(_.map(GQCharacterDataHistory.toCharacterData))
          } yield ((mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map{ case (c, amount) => GQCharactersRankByEarned(c.key, c.owner, c.`class`, c.level, amount) }
      }
    } yield ranks
  }

  def classifyHighEarnChar(history: Seq[GQCharacterGameHistory], limit: Int): Seq[(String, (UUID, Double))] = {
    val characters = HashMap.empty[String, (UUID, Double)]
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

  def getCharacterByUserAndID(user: UUID, id: String): Future[JsValue] = {
    for {
      // get all characters that are still alive
      alive <- charDataRepo.getByUserAndID(user, id)
      // get all characters that are already eliminated and
      // convert to GQCharacterData from GQCharacterDataHistory
      eliminated <- charDataRepo
        .getCharacterHistoryByUserAndID(user, id)
        .map(_.map(GQCharacterDataHistory.toCharacterData))

      // merge two Future[Seq] in single Future[Seq]
      logs <- {
        val characters: Seq[GQCharacterData] = mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)

        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                  v.id,
                  List(GQGameStatus(v.winner, v.winnerID, true),
                      GQGameStatus(v.loser, v.loserID, false)),
                  v.logs,
                  v.timeExecuted)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }

  def getAliveCharacters(user: UUID): Future[JsValue] = {
    for {
      characters <- charDataRepo.getByUser(user)

      logs <- {
        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                  v.id,
                  List(GQGameStatus(v.winner, v.winnerID, true),
                      GQGameStatus(v.loser, v.loserID, false)),
                  v.logs,
                  v.timeExecuted)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }

  def getCharacterDataByID(id: String): Future[JsValue] = {
      for {
        character <- charDataRepo.getByID(id)

        logs <- {
          // iterate each characters games history..
          character.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                  v.id,
                  List(GQGameStatus(v.winner, v.winnerID, true),
                      GQGameStatus(v.loser, v.loserID, false)),
                  v.logs,
                  v.timeExecuted)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs
            .map(v => (character.toJson, Json.toJson(v)))
            .map(Json.toJson(_))
          })
          .getOrElse(Future(JsNull))
        }

      } yield logs
    }

  def getCharacterHistoryByUserAndID(user: UUID, id: String): Future[JsValue] = {
      for {
        characters <- charDataRepo.getCharacterHistoryByUserAndID(user, id)

        logs <- {
          // iterate each characters games history..
          val tupled: Future[Seq[(JsValue, JsValue)]] =
            Future.sequence(characters.map({ character =>
              // get all history of character
              val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
              val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
                seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                  v.id,
                  List(GQGameStatus(v.winner, v.winnerID, true),
                      GQGameStatus(v.loser, v.loserID, false)),
                  v.logs,
                  v.timeExecuted)))
              // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
              seqLogs.map(v => (character.toJson, Json.toJson(v)))
            }))

          // convert Seq[JSON] to JsArray
          tupled.map(x => Json.toJson(x))
        }

      } yield logs
    }

  def getAllEliminatedCharacters(user: UUID): Future[JsValue] = {
    for {
      characters <- charDataRepo
        .getHistoryByUser(user)
        .map(_.map(GQCharacterDataHistory.toCharacterData))

      logs <- {
        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.key)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(
                  v.id,
                  List(GQGameStatus(v.winner, v.winnerID, true),
                      GQGameStatus(v.loser, v.loserID, false)),
                  v.logs,
                  v.timeExecuted)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }

  // TOD: scheduled process (Weekly for Lifetime Win Streak)
  // get overall history in a week, process and save it to WinStreak tbl
  // charDataRepo.getGameHistoryByDateRange(from, to)
  def separateHistoryByCharID(seq: Seq[GQCharacterGameHistory]): HashMap[String, ListBuffer[(String, Boolean, Long)]] = {
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

  def winStreakPerDay(): Future[List[GQCharactersRankByWinStreak]] = {
    val today: Long = Instant.now().getEpochSecond
    for {
      history <- gameHistoryRepo.getGameHistoryByDateRange(today - (24*60*60), today)
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }

  def winStreakPerWeekly(): Future[List[GQCharactersRankByWinStreak]] = {
    val today: Long = Instant.now().getEpochSecond
    for {
      history <- gameHistoryRepo.getGameHistoryByDateRange(today - ((24*60*60) * 7), today)
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }

  def winStreakLifeTime(): Future[List[GQCharactersRankByWinStreak]] = {
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
  def calcStreakToStreakObject(v: HashMap[String,Int]): Future[List[GQCharactersRankByWinStreak]] =
    Future.sequence {
      v.map { v =>
        for {
          al <- charDataRepo.getByID(v._1)
          el <- charDataRepo.getCharacterHistoryByID(v._1)
          either <- Future.successful(al.getOrElse(el.get))
          winstreak <- Future.successful {
            GQCharactersRankByWinStreak(v._1,
                                        either.owner,
                                        either.`class`,
                                        either.level,
                                        v._2)
          }
        } yield winstreak
      }.toList
    }
    .map(_.filterNot(_.win_streak == 0).sortBy(- _.win_streak).take(10))
}