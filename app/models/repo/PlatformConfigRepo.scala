package models.repo

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain._

@Singleton
class PlatformConfigRepo @Inject()(
    dao: models.dao.PlatformConfigDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  // do not add if table is not empty//
  def add(config: PlatformConfig): Future[Int] = {
    for {
      isEmpty <- default()
      process <- isEmpty.map(_ => Future.successful(1)).getOrElse(db.run(dao.Query += config))
    } yield (process)
  }
  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)
  def update(config: PlatformConfig): Future[Int] =
    db.run(dao.Query(config.id).update(config))
  def default(): Future[Option[PlatformConfig]] =
    db.run(dao.Query.result.headOption)

  def updateOrAddGame(game: PlatformGame): Future[Int] = {
    for {
      config <- default()
      games <- Future.successful(config.map(_.games).getOrElse(List.empty))
      // find the game that will be updated
      update <- {
        val prevConfig: PlatformConfig = config.get
        games
          .find(_.id == game.id)
          .map { v =>
            // remove old game config and insert the new config
            val newGameConfig: List[PlatformGame] = prevConfig.games.filter(_.id != game.id) :+ game
            // update previous config
            val updatedConfig: PlatformConfig = prevConfig.copy(games = newGameConfig)
            update(updatedConfig)
          }
          .getOrElse({
            // update previous config
            val updatedConfig: PlatformConfig = prevConfig.copy(games = prevConfig.games :+ game)
            update(updatedConfig)
          })
      }
    } yield (update)
  }
  def updateHost(host: PlatformHost): Future[Int] = {
    for {
      config <- default()
      hosts <- Future.successful(config.map(_.hosts).getOrElse(List.empty))
      // find the host that will be updated
      update <- {
        val prevConfig: PlatformConfig = config.get
        hosts
          .find(_.name == host.name)
          .map { v =>
            // remove old game config and insert the new config
            val newGameConfig: List[PlatformHost] = prevConfig.hosts.filter(_.name != host.name) :+ host
            // update previous config
            val updatedConfig: PlatformConfig = prevConfig.copy(hosts = newGameConfig)
            update(updatedConfig)
          }
          .getOrElse({
            // update previous config
            val updatedConfig: PlatformConfig = prevConfig.copy(hosts = prevConfig.hosts :+ host)
            update(updatedConfig)
          })
      }
    } yield (update)
  }
}
