package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain.eosio._
import models.repo.eosio._

@Singleton
class GQGameService @Inject()(
      charDataRepo: GQCharacterDataRepo,
      // charDataHistoryRepo: GQCharacterDataHistoryRepo,
      charGameHistoryRepo: GQCharacterGameHistoryRepo ) {

  // def getCharacterWinAndLostHistory(user: String, characterID: String) = ???
  // def getGameLogs(gameID: String) = ???

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
  def highEarnCharactersAllTime(): Future[Seq[GQCharactersRank]] = {
    for {
      characters <- charDataRepo.dynamicDataSort("prize", 10).map(_.sortBy(-_.prize).take(10))
      process <- Future.successful(characters.map(c => GQCharactersRank(c.id, c.owner, c.ghost_level, c.ghost_class, c.prize)))
    } yield (process)
  }

  def highEarnCharactersDaily(): Future[Seq[GQCharactersRank]] = {
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
        getCharaterInfo.map{ case (c, amount) => GQCharactersRank(c.id, c.owner, c.ghost_level, c.ghost_class, amount) }
      }
    } yield (ranks)
  }

  def highEarnCharactersWeekly(): Future[Seq[GQCharactersRank]] = {
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
        getCharaterInfo.map{ case (c, amount) => GQCharactersRank(c.id, c.owner, c.ghost_level, c.ghost_class, amount) }
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
        characters <- charDataRepo.getByID(id)

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
}