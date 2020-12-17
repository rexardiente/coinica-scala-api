package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQCharacterDataHistory

@Singleton
final class GQCharacterDataHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharacterDataHistoryTable(tag: Tag) extends Table[GQCharacterDataHistory](tag, "GQ_CHARACTER_DATA_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def chracterID = column[Long] ("CHARACTER_ID")
    def player = column[String] ("PLAYER")
    def life = column[Int] ("ENEMY")
    def hp = column[Int] ("HP")
    def hitpoints = column[Int] ("HIT_POINTS")
    def ghostClass = column[Int] ("CLASS")
    def level = column[Int] ("LEVEL")
    def status = column[Int] ("STATUS")
    def attack = column[Int] ("ATTACT")
    def defense = column[Int] ("DEFENSE")
    def speed = column[Int] ("SPEED")
    def luck = column[Int] ("LUCK")
    def prize = column[String] ("PRIZE")
    def battleLimit = column[Int] ("BATTLE_LIMIT")
    def battleCount = column[Int] ("BATTLE_COUNT")
    def lastMatch = column[Long] ("LAST_MATCH")
    def matches = column[Int] ("MATCHES")

   def * = (id,
            chracterID,
            player,
            life,
            hp,
            hitpoints,
            ghostClass,
            level,
            status,
            attack,
            defense,
            speed,
            luck,
            prize,
            battleLimit,
            battleCount,
            lastMatch,
            matches) <> ((GQCharacterDataHistory.apply _).tupled, GQCharacterDataHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterDataHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(id: Long, player: String) = this.withFilter(x => x.chracterID === id && x.player === player)
  } 
}