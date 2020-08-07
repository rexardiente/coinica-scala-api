package models.repo.game

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.game.Game

@Singleton
class GameRepo @Inject()(
    dao: models.dao.game.GameDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(game: Game): Future[Int] =
    db.run(dao.Query += game)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(game: Game): Future[Int] =
    db.run(dao.Query.filter(_.id === game.id).update(game))

  def all(): Future[Seq[Game]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Game]] =
    db.run(dao.Query.filter(r => r.id === id)
      .result
      .headOption)

  def findByName(name: String): Future[Option[Game]] =
    db.run(dao.Query.filter(r => r.name === name)
      .result
      .headOption)
}
