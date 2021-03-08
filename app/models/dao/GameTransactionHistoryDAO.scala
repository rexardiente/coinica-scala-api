package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ GameTransactionHistory, TransactionType }

@Singleton
final class GameTransactionHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GameTransactionHistoryTable(tag: Tag) extends Table[GameTransactionHistory](tag, "GAME_TRANSACTION_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def gameID = column[UUID] ("GAME_ID")
    def game = column[String] ("GAME")
    def icon = column[String] ("ICON")
    def `type` = column[List[TransactionType]] ("TYPE")
    def isConfirmed = column[Boolean] ("IS_CONFIRMED")
    def createdAt = column[Instant] ("CREATED_AT")

   def * = (id, gameID, game, icon, `type`, isConfirmed, createdAt) <> ((GameTransactionHistory.apply _).tupled, GameTransactionHistory.unapply)
  }

  object Query extends TableQuery(new GameTransactionHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}

