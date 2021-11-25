package models.repo

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.repo.PlatformConfigRepo
import models.service.PlatformConfigService
import models.domain._

@Singleton
class GameRepo @Inject()(
    repo: PlatformConfigRepo,
    service: PlatformConfigService,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def all(): Future[List[PlatformGame]] = service.getGamesInfo

  def addOrUpdate(game: PlatformGame): Future[Int] = repo.updateOrAddGame(game)

  def exist(id: UUID): Future[Boolean] = service.isGameExists(id)

  def findByID(id: UUID): Future[Option[PlatformGame]] =
    service.getGameInfoByID(id)

  def findByName(name: String): Future[Option[PlatformGame]] =
    service.getGameInfoByName(name)
}
