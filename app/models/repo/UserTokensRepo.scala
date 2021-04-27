package models.repo

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.UserTokens

@Singleton
class UserTokensRepo @Inject()(
    dao: models.dao.UserTokensDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(user: UserTokens): Future[Int] =
    db.run(dao.Query += user)
  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)
  def update(user: UserTokens): Future[Int] =
    db.run(dao.Query.filter(_.id === user.id).update(user))
  def getByID(id: UUID): Future[Option[UserTokens]] =
    db.run(dao.Query(id).result.headOption)
  def exists(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)
  def hasValidLoginToken(id: UUID, token: String, limit: Long): Future[Boolean] =
    db.run(dao.Query.filter(x => x.id === id && x.token === token && x.login >= limit).exists.result)
  def hasValidEmailToken(id: UUID, token: String, limit: Long): Future[Boolean] =
    db.run(dao.Query.filter(x => x.id === id && x.token === token && x.email >= limit).exists.result)
  def hasValidPasswordToken(id: UUID, token: String, limit: Long): Future[Boolean] =
    db.run(dao.Query.filter(x => x.id === id && x.token === token && x.password >= limit).exists.result)
  def hasSession(id: UUID): Future[Boolean] =
    db.run(dao.Query.filter(x => x.id === id && x.token.isEmpty.?).exists.result)
  // def hasValidSession(id: UUID, token: String, currentTime: Long): Future[Boolean] =
  //   db.run(dao.Query.filter(x =>
  //       x.id === id &&
  //       x.token === token &&
  //       x.tokenLimit >= currentTime
  //     ).exists.result)
  // and make sure token is valid by checking if not expired..
  def getLoginByToken(token: String): Future[Option[UserTokens]] =
    db.run(dao.Query.filter(x => x.token === token && x.login >= Instant.now.getEpochSecond).result.headOption)
  // def getByNameAndSessionToken(username: String, token: String): Future[Option[UserAccount]] =
  //   db.run(dao.Query.filter(x => x.username === username && x.token === token).result.headOption)
}
