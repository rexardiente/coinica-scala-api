package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQCharacterData

@Singleton
final class GQCharacterDataDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharacterDataTable(tag: Tag) extends Table[GQCharacterData](tag, "GQ_CHARACTER_DATA") {
    def characterID = column[String] ("CHARACTER_ID", O.PrimaryKey)
    def owner = column[String] ("OWNER")
    def life = column[Int] ("LIFE")
    def initial_hp = column[Int] ("INITIAL_HP")
    def hitpoints = column[Int] ("HIT_POINTS")
    def `class` = column[Int] ("CLASS")
    def level = column[Int] ("LEVEL")
    def status = column[Int] ("STATUS")
    def attack = column[Int] ("ATTACK")
    def defense = column[Int] ("DEFENSE")
    def speed = column[Int] ("SPEED")
    def luck = column[Int] ("LUCK")
    def prize = column[String] ("PRIZE")
    def battle_limit = column[Int] ("BATTLE_LIMIT")
    def battle_count = column[Int] ("BATTLE_COUNT")
    def last_match = column[Long] ("LAST_MATCH")

    def * = (characterID,
            owner,
            life,
            initial_hp,
            hitpoints,
            `class`,
            level,
            status,
            attack,
            defense,
            speed,
            luck,
            prize,
            battle_limit,
            battle_count,
            last_match) <> ((GQCharacterData.apply _).tupled, GQCharacterData.unapply)
  }

  object Query extends TableQuery(new GQCharacterDataTable(_)) {
    def apply(id: String) = this.withFilter(_.characterID === id)
  } 
}