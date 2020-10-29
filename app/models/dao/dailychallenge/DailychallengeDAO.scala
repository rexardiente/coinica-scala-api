package models.dao.dailychallenge

import java.util.UUID
import java.time.Instant

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.dailychallenge.Dailychallenge

@Singleton
final class DailychallengeDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class DailychallengeTable(tag: Tag) extends Table[Dailychallenge](tag, "DAILYCHALLENGE") {
    def id = column[UUID] ("ID")
    def gamename = column[String] ("GAMENAME")   
    def rankname = column[Option[String]] ("RANKNAME")
   def challengedate =column[Instant] ("CHALLENGEDATE")

    def rankreward = column[String] ("RANKREWARD")
    def * = (id, gamename, rankname,  rankreward, challengedate) <> ((Dailychallenge.apply _).tupled, Dailychallenge.unapply) 
  }

  object Query extends TableQuery(new DailychallengeTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}