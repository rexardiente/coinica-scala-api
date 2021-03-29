package models.dao

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ OverAllGameHistory, TransactionType }

@Singleton
final class OverAllGameHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class OverAllGameHistoryTable(tag: Tag) extends Table[OverAllGameHistory](tag, "OVER_ALL_GAME_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def tx_hash = column[String] ("TX_HASH")
    def gameID = column[UUID] ("GAME_ID")
    def game = column[String] ("GAME")
    def `type` = column[TransactionType] ("TYPE")
    def isConfirmed = column[Boolean] ("IS_CONFIRMED")
    def createdAt = column[Long] ("CREATED_AT")

   def * = (id,
            tx_hash,
            gameID,
            game,
            `type`,
            isConfirmed,
            createdAt) <> ((OverAllGameHistory.apply _).tupled, OverAllGameHistory.unapply)
  }

  object Query extends TableQuery(new OverAllGameHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}

