package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQ.v2.GQCharacterData

@Singleton
final class GQCharacterDataDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharacterDataTable(tag: Tag)
            extends Table[GQCharacterData](tag, "GQ_CHARACTER_DATA")
            with models.service.DynamicSortBySupport.ColumnSelector {
    def key = column[String] ("CHARACTER_ID", O.PrimaryKey)
    def owner = column[UUID] ("PLAYER")
    def life = column[Int] ("LIFE")
    def hp = column[Int] ("HP")
    def ghostClass= column[Int] ("CLASS")
    def level = column[Int] ("LEVEL")
    def status = column[Int] ("STATUS")
    def attack = column[Int] ("ATTACK")
    def defense = column[Int] ("DEFENSE")
    def speed = column[Int] ("SPEED")
    def luck = column[Int] ("LUCK")
    def limit = column[Int] ("BATTLE_LIMIT")
    def count = column[Int] ("BATTLE_COUNT")
    def isNew = column[Boolean] ("IS_NEW")
    def createdAt = column[Long] ("CREATED_AT")

    def * = (key,
            owner,
            life,
            hp,
            ghostClass,
            level,
            status,
            attack,
            defense,
            speed,
            luck,
            limit,
            count,
            isNew,
            createdAt) <> ((GQCharacterData.apply _).tupled, GQCharacterData.unapply)

    val select = Map("key" -> (this.key),
                    "owner" -> (this.owner),
                    "life" -> (this.life),
                    "hp" -> (this.hp),
                    "ghostClass" -> (this.ghostClass),
                    "level" -> (this.level),
                    "status" -> (this.status),
                    "attack" -> (this.attack),
                    "defense" -> (this.defense),
                    "speed" -> (this.speed),
                    "luck" -> (this.luck),
                    "limit" -> (this.limit),
                    "count" -> (this.count),
                    "isNew" -> (this.isNew),
                    "createdAt" -> (this.createdAt))
  }

  object Query extends TableQuery(new GQCharacterDataTable(_)) {
    def apply(key: String) = this.withFilter(_.key === key)
  }
}