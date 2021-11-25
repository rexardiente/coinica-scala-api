package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain._
import models.repo.PlatformConfigRepo
import utils.SystemConfig

@Singleton
class PlatformConfigService @Inject()(configRepo: PlatformConfigRepo) {
  // TODO: setup default data to make sure its working if DB is not setup
  // set as function to make sure changes on DB will reflect realtime..
  private def loadConfig(): Future[Option[PlatformConfig]] = configRepo.default()
  // System Configs
  def getSystemScheduler(): Future[Int] =
    loadConfig().map(_.map(_.defaultscheduler).getOrElse(SystemConfig.DEFAULT_SYSTEM_SCHEDULER_TIMER))
  def getDefaultWeiValue(): Future[BigDecimal] =
    loadConfig().map(_.map(x => BigDecimal(x.wei)).getOrElse(SystemConfig.DEFAULT_WEI_VALUE))
  def getSupportedCurrencies(): Future[List[PlatformCurrency]] = {
    for {
      config <- loadConfig()
      currencies <- Future.successful(config.map(_.currencies).getOrElse(List.empty))
    } yield (currencies)
  }
  def isExistsCurrency(name: String): Future[Boolean] = {
    for {
      currencies <- getSupportedCurrencies()
      currencie <- Future.successful {
        currencies.find(_.name == name).map(_ => true).getOrElse(false)
      }
    } yield (currencie)
  }
  def getCurrencyByName(name: String): Future[Option[PlatformCurrency]] = {
    for {
      currencies <- getSupportedCurrencies()
      currencie <- Future.successful(currencies.find(_.name == name))
    } yield (currencie)
  }
  def getCurrencyBySymbol(symbol: String): Future[Option[PlatformCurrency]] = {
    for {
      currencies <- getSupportedCurrencies()
      currencie <- Future.successful(currencies.find(_.symbol == symbol))
    } yield (currencie)
  }
  def getHosts(): Future[List[PlatformHost]] = {
    for {
      config <- loadConfig()
      hosts <- Future.successful(config.map(_.hosts).getOrElse(List.empty))
    } yield (hosts)
  }
  def getHostByName(name: String): Future[Option[PlatformHost]] = {
    for {
      hosts <- getHosts()
      host <- Future.successful(hosts.find(_.name == name))
    } yield (host)
  }

  // Game Configs
  def getGamesInfo(): Future[List[PlatformGame]] = {
    for {
      config <- loadConfig()
      games <- Future.successful(config.map(_.games).getOrElse(List.empty))
    } yield (games)
  }
  def isGameExists(id: UUID): Future[Boolean] = {
    for {
      games <- getGamesInfo()
      isExists <- Future.successful(games.find(_.id == id).map(_ => true).getOrElse(false))
    } yield (isExists)
  }
  def getGameInfoByName(name: String): Future[Option[PlatformGame]] = {
    for {
      games <- getGamesInfo()
      game <- Future.successful(games.find(_.name == name))
    } yield (game)
  }
  def getGameInfoByID(id: UUID): Future[Option[PlatformGame]] = {
    for {
      games <- getGamesInfo()
      game <- Future.successful(games.find(_.id == id))
    } yield (game)
  }
  def getGameCode(id: UUID): Future[Option[String]] =
    getGameInfoByID(id).map(_.map(_.code))
  def getGameDisplayName(id: UUID): Future[Option[String]] =
    getGameInfoByID(id).map(_.map(_.displayName))
  def getGameName(id: UUID): Future[Option[String]] =
    getGameInfoByID(id).map(_.map(_.name))

  def getGameCodeByName(name: String): Future[Option[String]] =
    getGameInfoByName(name).map(_.map(_.code))
  def getGameDisplayNameByName(name: String): Future[Option[String]] =
    getGameInfoByName(name).map(_.map(_.displayName))
  def getGameNameByName(name: String): Future[Option[String]] =
    getGameInfoByName(name).map(_.map(_.name))
  def getGameIDByName(name: String): Future[Option[UUID]] =
    getGameInfoByName(name).map(_.map(_.id))
}