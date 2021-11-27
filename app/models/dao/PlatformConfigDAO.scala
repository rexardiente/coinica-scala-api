package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ PlatformConfig, PlatformGame, PlatformHost, PlatformCurrency }

@Singleton
final class PlatformConfigDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class PlatformConfigTable(tag: Tag) extends Table[PlatformConfig](tag, "PLATFORM_CONFIG") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def games = column[List[PlatformGame]] ("GAMES")
    def hosts = column[List[PlatformHost]] ("HOSTS")
    def currencies = column[List[PlatformCurrency]] ("CURRENCIES")
    def wei = column[String] ("WEI_VALUE")
    def tokenExpiration = column[Int] ("TOKEN_EXPIRATION")
    def mailerExpiration = column[Int] ("MAILER_EXPIRATION")
    def defaultscheduler = column[Int] ("DEFAULT_SCHEDULER")
    def updatedAt = column[Instant] ("UPDATED_AT")

    def * = (id,
    				games,
    				hosts,
            currencies,
    				wei,
    				tokenExpiration,
    				mailerExpiration,
            defaultscheduler,
            updatedAt) <> (PlatformConfig.tupled, PlatformConfig.unapply)
  }

  object Query extends TableQuery(new PlatformConfigTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
