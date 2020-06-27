package models.repo.game

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.game.Genre

@Singleton
class GenreRepo @Inject()(
    dao: models.dao.game.GenreDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(genre: Genre): Future[Int] =
    db.run(dao.Query += genre)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(genre: Genre): Future[Int] =
    db.run(dao.Query.filter(_.id === genre.id).update(genre))

  def all(): Future[Seq[Genre]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Genre]] =
    db.run(dao.Query.filter(r => r.id === id)
      .result
      .headOption)

  def findByName(name: String): Future[Option[Genre]] =
    db.run(dao.Query.filter(r => r.name === name)
      .result
      .headOption)
}
