package models.dao.game

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.game.Game

@Singleton
final class GameDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class GameTable(tag: Tag) extends Table[Game](tag, "GAME") {
    def id = column[UUID] ("ID")
    def name = column[String] ("NAME")
    def imgURl = column[String] ("IMG_URL")
    def genre = column[UUID] ("GENRE")
    def description = column[Option[String]] ("DESCRIPTION")

   def * = (id, name, imgURl, genre, description) <> ((Game.apply _).tupled, Game.unapply)
  }

  object Query extends TableQuery(new GameTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}

