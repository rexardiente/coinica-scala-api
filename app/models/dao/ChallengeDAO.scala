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
    def bets = column[Double] ("BETS")
    def profit = column[Double] ("PROFIT")
    def ratio = column[Double] ("RATIO")
    def vippoints = column[Double] ("VIPPOINTS")
    def challengecreated = column[Long] ("CHALLENGECREATED")

    def * = (id, name, bets, profit, ratio, vippoints, challengecreated) <> ((Challenge.apply _).tupled, Challenge.unapply) 
  }

  object Query extends TableQuery(new ChallengeTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
