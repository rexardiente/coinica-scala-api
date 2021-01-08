package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
  def getCharacterDataAndHistoryLogsByUser(user: String): Future[JsValue] = {
    for {
      // get all characters that are still alive
      alive <- charDataRepo.findByUser(user)
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
              seqHistory.map(_.map(v => new GQCharacterDataHistoryLogs(v.id, v.status, v.log)))
            // Future[(GQCharacterData, Seq[GQCharacterDataHistoryLogs])] and convert to JSON values
            seqLogs.map(v => (character.toJson, Json.toJson(v)))
          }))

        // convert Seq[JSON] to JsArray
        tupled.map(x => Json.toJson(x))
      }

    } yield logs
  }
}