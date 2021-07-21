package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ GhostQuestBattleResult, GhostQuestCharacterValue, GhostQuestCharacterGameLog }

@Singleton
final class GhostQuestBattleResultDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GhostQuestBattleResultTable(tag: Tag) extends Table[GhostQuestBattleResult](tag, "GHOST_QUEST_CURRENT_BATTLE") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def characters = column[List[(String, (Int, Boolean))]] ("CHARACTERS")
    def logs = column[List[GhostQuestCharacterGameLog]] ("GAME_LOGS")

    def * = (id, characters, logs) <> ((GhostQuestBattleResult.apply _).tupled, GhostQuestBattleResult.unapply)
  }

  object Query extends TableQuery(new GhostQuestBattleResultTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def clearTbl = this.delete
  }
}