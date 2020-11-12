package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Game

@Singleton
final class GameDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class GameTable(tag: Tag) extends Table[Game](tag, "GAME") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def imgURl = column[String] ("IMG_URL")
    def path = column[String] ("PATH")
    def genre = column[UUID] ("GENRE")
    def description = column[Option[String]] ("DESCRIPTION")

   def * = (id, name, imgURl, path, genre, description) <> ((Game.apply _).tupled, Game.unapply)
  
  }

  object Query extends TableQuery(new GameTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
  
}
