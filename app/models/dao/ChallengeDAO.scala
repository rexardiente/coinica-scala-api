package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Challenge

@Singleton
final class ChallengeDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class ChallengeTable(tag: Tag) extends Table[Challenge](tag, "CHALLENGE") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def description = column[String] ("DESCRIPTION")
    def startAt = column[Instant] ("START_AT")
    def expireAt = column[Instant] ("EXPIRE_AT")
    def isAvailable = column[Boolean] ("IS_AVAILABLE")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (id, name, description, startAt, expireAt, isAvailable, createdAt) <> (Challenge.tupled, Challenge.unapply)
  }

  object Query extends TableQuery(new ChallengeTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}