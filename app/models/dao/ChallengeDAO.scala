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
    def gameID = column[UUID] ("GAME_ID")
    def description = column[String] ("DESCRIPTION")
    def createdAt = column[Instant] ("CREATED_AT")
    def expiredAt = column[Instant] ("EXPIRE_AT")

    def * = (id, gameID, description, createdAt, expiredAt) <> (Challenge.tupled, Challenge.unapply)
  }

  object Query extends TableQuery(new ChallengeTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(createdAt: Instant) = this.withFilter(_.createdAt === createdAt)
  }
}