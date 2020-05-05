package models.repo.user.info

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.user.info.User

@Singleton
class UserRepo @Inject()(
    dao: models.dao.user.info.UserDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(user: User): Future[Int] =
    db.run(dao.Query += user)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(user: User): Future[Int] =
    db.run(dao.Query.filter(_.id === user.id).update(user))

  def all(): Future[Seq[User]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def find(id: UUID, account: String): Future[Option[User]] =
    db.run(dao.Query.filter(r => r.id === id && r.account === account)
      .result
      .headOption)
}
