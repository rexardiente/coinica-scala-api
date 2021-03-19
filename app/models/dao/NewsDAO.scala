package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.News

@Singleton
final class NewsDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class NewsTable(tag: Tag) extends Table[News](tag, "NEWS") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def title = column[String] ("TITLE")
    def subTitle = column[String] ("SUB_TITLE")
    def description = column[String] ("DESCRIPTION")
    def author = column[String] ("AUTHOR")
    def url = column[String] ("URL")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (id,
            title,
            subTitle,
            description,
            author,
            url,
            createdAt) <> (News.tupled, News.unapply)
  }

  object Query extends TableQuery(new NewsTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(title: String) = this.withFilter(_.title === title)
  }
}
