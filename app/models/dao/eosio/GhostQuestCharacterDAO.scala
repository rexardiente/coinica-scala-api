package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ GhostQuestCharacter, GhostQuestCharacterValue }

@Singleton
final class GhostQuestCharacterDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GhostQuestCharacterTable(tag: Tag) extends Table[GhostQuestCharacter](tag, "GHOST_QUEST_CHARACTERS") {
    def key = column[String] ("KEY", O.PrimaryKey)
    def value = column[GhostQuestCharacterValue] ("VALUE")

    def * = (key, value) <> ((GhostQuestCharacter.apply _).tupled, GhostQuestCharacter.unapply)
  }

  object Query extends TableQuery(new GhostQuestCharacterTable(_)) {
    def apply(key: String) = this.withFilter(_.key === key)
    def clearTbl = this.delete
  }
}