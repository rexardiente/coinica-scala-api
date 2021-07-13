package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.libs.json.JsValue
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ GhostQuestCharacterHistory }

@Singleton
final class GhostQuestCharacterHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GhostQuestCharacterHistoryTable(tag: Tag) extends Table[GhostQuestCharacterHistory](tag, "GHOST_QUEST_CHARACTERS") {
    def key = column[String] ("KEY")
    def owner_id = column[Int] ("OWNER_ID")
    def ghost_name = column[String] ("GHOST_NAME")
    def ghost_id = column[Int] ("GHOST_ID")
    def rarity = column[Int] ("RARITY")
    def character_life = column[Int] ("LIFE")
    def initial_hp = column[Int] ("INITIAL_HP")
    def hitpoints = column[Int] ("HIT_POINTS")
    def status = column[Int] ("STATUS")
    def attack = column[Int] ("ATTACK")
    def defense = column[Int] ("DEFENSE")
    def speed = column[Int] ("SPEED")
    def luck = column[Int] ("LUCK")
    def prize = column[BigDecimal] ("PRIZE")
    def battle_limit = column[Int] ("BATTLE_LIMIT")
    def battle_count = column[Int] ("BATTLE_COUNT")
    def last_match = column[Int] ("LAST_MATCH")
    def enemy_fought = column[JsValue] ("MATCHES")

    def * = (key,
            owner_id,
            ghost_name,
            ghost_id,
            rarity,
            character_life,
            initial_hp,
            hitpoints,
            status,
            attack,
            defense,
            speed,
            luck,
            prize,
            battle_limit,
            battle_count,
            last_match,
            enemy_fought) <> ((GhostQuestCharacterHistory.apply _).tupled, GhostQuestCharacterHistory.unapply)
  }

  object Query extends TableQuery(new GhostQuestCharacterHistoryTable(_)) {
    def apply(key: String) = this.withFilter(_.key === key)
    def apply(key: String, owner_id: Int) = this.withFilter(x => x.key === key && x.owner_id === owner_id)
  }
}