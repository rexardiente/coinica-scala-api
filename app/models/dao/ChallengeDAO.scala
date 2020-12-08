package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Challenge

@Singleton
final class ChallengeDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._


  protected class ChallengeTable(tag: Tag) extends Table[Challenge](tag, "CHALLENGE") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def gameID = column[UUID] ("GAME_ID") 
    def reward = column[Double] ("REWARD")
    def rankname = column[String] ("RANKNAME")
    def challengecreated = column[Long] ("CHALLENGECREATED")

    def * = (id, name, gameID, reward, rankname, challengecreated) <> ((Challenge.apply _).tupled, Challenge.unapply) 
  }

  object Query extends TableQuery(new ChallengeTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
