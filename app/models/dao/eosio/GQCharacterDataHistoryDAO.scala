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
    def chracterID = column[String] ("CHARACTER_ID", O.PrimaryKey)
    def player = column[String] ("PLAYER")
    def life = column[Int] ("ENEMY")
    def hp = column[Int] ("HP")
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

    def * = (chracterID,
            player,
            life,
            hp,
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
            lastMatch) <> ((GQCharacterDataHistory.apply _).tupled, GQCharacterDataHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterDataHistoryTable(_)) {
    def apply(id: String) = this.withFilter(_.chracterID === id)
    def apply(id: String, player: String) = this.withFilter(x => x.chracterID === id && x.player === player)
  } 
}