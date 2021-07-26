package models.service

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.domain.eosio.{ GhostQuestCharacter, GhostQuestCharacterHistory, GhostQuestCharacterGameHistory }
import models.repo.eosio.{ GhostQuestCharacterRepo, GhostQuestCharacterHistoryRepo, GhostQuestCharacterGameHistoryRepo }

@Singleton
class GhostQuestCharacterService @Inject()(
          data: GhostQuestCharacterRepo,
          characterHistory: GhostQuestCharacterHistoryRepo,
          gameHistory: GhostQuestCharacterGameHistoryRepo) {
  def getGameHistoryByDateRange(from: Long, to: Long): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getGameHistoryByDateRange(from, to)

  def getAllGameHistory(): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getAllGameHistory()

  def insertGameHistory(v: GhostQuestCharacterGameHistory): Future[Int] =
    gameHistory.insertGameHistory(v)

  def allGameHistory(): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.allGameHistory()

  def existGameHistory(id: String): Future[Boolean] =
    gameHistory.existGameHistory(id)

  def filteredGameHistoryByID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.filteredGameHistoryByID(id)

  def getGameHistoryByUserID(ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getGameHistoryByUserID(ownerID)

  def getGameHistoryByUsernameCharacterIDAndDate(id: String, ownerID: Int, startDate: Long, endDate: Long): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getGameHistoryByUsernameCharacterIDAndDate(id, ownerID, startDate, endDate)

  def getGameHistoryByGameIDAndCharacterID(id: String, ownerID: Int): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getGameHistoryByGameIDAndCharacterID(id, ownerID)

  def getGameHistoryByCharacterID(id: String): Future[Seq[GhostQuestCharacterGameHistory]] =
    gameHistory.getGameHistoryByCharacterID(id)

  def insertGhostQuestCharacter(v: GhostQuestCharacter): Future[Int] =
    data.insert(v)

  def insertGhostQuestCharacters(v: Seq[GhostQuestCharacter]): Future[Seq[Int]] =
    Future.sequence(v.map(data.insert(_)))

  def removeAllGhostQuestCharacter(): Future[Int] =
    data.removeAll()

  def updateGhostQuestCharacter(v: GhostQuestCharacter): Future[Int] =
    data.update(v)

  def allGhostQuestCharacter(): Future[Seq[GhostQuestCharacter]] =
    data.all()

  def existGhostQuestCharacter(key: String): Future[Boolean] =
    data.exist(key)

  def findGhostQuestCharacter(key: String): Future[Option[GhostQuestCharacter]] =
    data.find(key)

  def insertGhostQuestCharacterHistory(data: GhostQuestCharacterHistory): Future[Int] =
    characterHistory.insert(data)

  def removeGhostQuestCharacterHistory(id: Int, key: String): Future[Int] =
    characterHistory.remove(id, key)

  def updateGhostQuestCharacterHistory(data: GhostQuestCharacterHistory): Future[Int] =
    characterHistory.update(data)

  def allGhostQuestCharacterHistory(): Future[Seq[GhostQuestCharacterHistory]] =
    characterHistory.all()

  def existGhostQuestCharacterHistory(key: String): Future[Boolean] =
    characterHistory.exist(key)

  def existGhostQuestCharacterHistory(key: String, ownerID: Int): Future[Boolean] =
    characterHistory.exist(key, ownerID)

  def findGhostQuestCharacterHistory(key: String): Future[Option[GhostQuestCharacterHistory]] =
    characterHistory.find(key)
  def findGhostQuestCharacterHistoryByOwnerID(ownerID: Int): Future[Seq[GhostQuestCharacterHistory]] =
    characterHistory.findByOwnerID(ownerID)
  def getGhostQuestCharacterHistoryByOwnerIDAndID(ownerID: Int, key: String): Future[Seq[GhostQuestCharacterHistory]] =
    characterHistory.findByOwnerIDAndID(ownerID, key)
  def getGhostQuestCharacterHistoryByKey(key: String): Future[Seq[GhostQuestCharacterHistory]] =
    characterHistory.findByKey(key)
}