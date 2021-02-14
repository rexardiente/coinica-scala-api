package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.mutable.{ HashMap, ListBuffer }
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain.eosio._
import models.repo.eosio._

@Singleton
class GQGameService @Inject()(
      charDataRepo: GQCharacterDataRepo,
      // charDataHistoryRepo: GQCharacterDataHistoryRepo,
      charGameHistoryRepo: GQCharacterGameHistoryRepo ) {

  def getHistoryByCharacterID(id: String): Future[Seq[GQCharacterGameHistory]] = {
    charGameHistoryRepo.getByCharacterID(id)
  }

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  // get all characters from specific player
  // characters are separate by status
  def getAllCharactersDataAndHistoryLogsByUser(user: String): Future[JsValue] = {
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
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
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
      characters <- charDataRepo.dynamicDataSort("prize", 10).map(_.sortBy(-_.prize).take(10))
      process <- Future.successful(characters.map(c => GQCharactersRankByEarned(c.id, c.owner, c.ghost_level, c.ghost_class, c.prize)))
    } yield (process)
  }

  def highEarnCharactersDaily(): Future[Seq[GQCharactersRankByEarned]] = {
    for {
      perDay <- charDataRepo.highestPerWeekOrDay((24*60*60), 10)
      getCharaterInfo <- Future.sequence {
        perDay.map { case (id, (player, amount)) =>
          for {
            alive <- charDataRepo.getByUserAndID(player, id)
            eliminated <- charDataRepo
              .getCharacterHistoryByUserAndID(player, id)
              .map(_.map(GQCharacterDataHistory.toCharacterData))
          } yield ((mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map{ case (c, amount) => GQCharactersRankByEarned(c.id, c.owner, c.ghost_level, c.ghost_class, amount) }
      }
    } yield (ranks)
  }

  def highEarnCharactersWeekly(): Future[Seq[GQCharactersRankByEarned]] = {
    for {
      perWeek <- charDataRepo.highestPerWeekOrDay((24*60*60) * 7, 10)
      getCharaterInfo <- Future.sequence {
        perWeek.map { case (id, (player, amount)) =>
          for {
            alive <- charDataRepo.getByUserAndID(player, id)
            eliminated <- charDataRepo
              .getCharacterHistoryByUserAndID(player, id)
              .map(_.map(GQCharacterDataHistory.toCharacterData))
          } yield ((mergeSeq[GQCharacterData, Seq[GQCharacterData]](alive, eliminated)).head, amount)
        }
      }
      ranks <- Future.successful {
        getCharaterInfo.map{ case (c, amount) => GQCharactersRankByEarned(c.id, c.owner, c.ghost_level, c.ghost_class, amount) }
      }
    } yield (ranks)
  }

  def getCharacterByUserAndID[T <: String](user: T, id: T): Future[JsValue] = {
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
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }

  def getAliveCharacters(user: String): Future[JsValue] = {
    for {
      characters <- charDataRepo.getByUser(user)

      logs <- {
        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
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
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs
            .map(v => (character.toJson, Json.toJson(v)))
            .map(Json.toJson(_))
          })
          .getOrElse(Future(JsNull))
        }

      } yield logs
    }

  def getCharacterHistoryByUserAndID[T <: String](user: T, id: T): Future[JsValue] = {
      for {
        characters <- charDataRepo.getCharacterHistoryByUserAndID(user, id)

        logs <- {
          // iterate each characters games history..
          val tupled: Future[Seq[(JsValue, JsValue)]] =
            Future.sequence(characters.map({ character =>
              // get all history of character
              val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
              val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
                seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
              // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
              seqLogs.map(v => (character.toJson, Json.toJson(v)))
            }))

          // convert Seq[JSON] to JsArray
          tupled.map(x => Json.toJson(x))
        }

      } yield logs
    }

  def getAllEliminatedCharacters(user: String): Future[JsValue] = {
    for {
      characters <- charDataRepo
        .getHistoryByUser(user)
        .map(_.map(GQCharacterDataHistory.toCharacterData))

      logs <- {
        // iterate each characters games history..
        val tupled: Future[Seq[(JsValue, JsValue)]] =
          Future.sequence(characters.map({ character =>
            // get all history of character
            val seqHistory: Future[Seq[GQCharacterGameHistory]] = getHistoryByCharacterID(character.id)
            val seqLogs: Future[Seq[GQCharacterDataHistoryLogs]] =
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.timeExecuted, v.log)))
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
  // charDataRepo.historyByDateRange(from, to)
  def separateHistoryByCharID(seq: Seq[GQCharacterGameHistory]): HashMap[String, ListBuffer[(String, Boolean, Long)]] = {
    val counter = HashMap.empty[String, ListBuffer[(String, Boolean, Long)]]
    // process -> seq of history
    seq.foreach(history => {
      // separate all losers and winners with game ID
      history.status.foreach(status => {
        // GameID, isWin, timeExecuted
        val id = status.char_id
        val isWin = if (status.isWin) true else false

        counter.addOne(id -> {
          if (counter.exists(_._1 == id))
            counter(id) += ((history.id, isWin, history.timeExecuted))
          else
            ListBuffer(("default1", isWin, history.timeExecuted))
        })
      })
    })
    // return list of charactes
    counter
  }

  def calcWinStreak(characters: HashMap[String, ListBuffer[(String, Boolean, Long)]]): HashMap[String, Int] = {
    characters.map { character =>
      val status       : ListBuffer[(String, Boolean, Long)] = character._2
      val streakCounter: ListBuffer[Int] = ListBuffer.empty[Int]
      val tempList     : ListBuffer[Int] = ListBuffer.empty[Int]

      status.zipWithIndex.map {
        case (v, i) =>
          if (v._2)
            tempList.addOne(i)
          else {
            // overall charcter info in winstreak will be shown for simplicity
            // updateCharacters.addOne(character._1 -> {
            //   if (updateCharacters.exists(_._1 == character._1))
            // })
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
      history <- charDataRepo.historyByDateRange(today - (24*60*60), today)
      separatedHistory <- Future.successful(separateHistoryByCharID(history))
      calcWinStreak <- Future.successful(calcWinStreak(separatedHistory))
      result <- calcStreakToStreakObject(calcWinStreak)
    } yield result
  }

  def winStreakPerWeekly(): Future[List[GQCharactersRankByWinStreak]] = {
    val today: Long = Instant.now().getEpochSecond
    for {
      history <- charDataRepo.historyByDateRange(today - ((24*60*60) * 7), today)
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
                                        either.ghost_level,
                                        either.ghost_class,
                                        v._2)
          }
        } yield winstreak
      }.toList
    }
    .map(_.filterNot(_.win_streak == 0).sortBy(- _.win_streak).take(10))

  def winStreakLifeTime(): Unit = ???
}