package models.dao.game

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.game.Genre

@Singleton
final class GenreDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class GenreTable(tag: Tag) extends Table[Genre](tag, "GENRE") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def description = column[Option[String]] ("DESCRIPTION")

   def * = (id, name, description) <> ((Genre.apply _).tupled, Genre.unapply)
  }

  object Query extends TableQuery(new GenreTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}

