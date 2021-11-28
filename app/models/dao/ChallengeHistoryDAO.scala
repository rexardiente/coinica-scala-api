package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ ChallengeHistory, ChallengeTracker }

@Singleton
final class ChallengeHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class ChallengeHistoryTable(tag: Tag) extends Table[ChallengeHistory](tag, "CHALLENGE_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def users = column[Seq[ChallengeTracker]] ("USERS")
    def createdAt = column[Long] ("CREATED_AT", O.Unique)

    def * = (id, users, createdAt) <> (ChallengeHistory.tupled, ChallengeHistory.unapply)
  }

  object Query extends TableQuery(new ChallengeHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}