package models.repo

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.UserAccount

@Singleton
class UserAccountRepo @Inject()(
    dao: models.dao.UserAccountDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(user: UserAccount): Future[Int] =
    db.run(dao.Query += user)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(user: UserAccount): Future[Int] =
    db.run(dao.Query.filter(_.id === user.id).update(user))

  def all(): Future[Seq[UserAccount]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def getByID(id: UUID): Future[Option[UserAccount]] =
    db.run(dao.Query(id).result.headOption)

  def isCodeExist(code: String): Future[Boolean] =
    db.run(dao.Query.filter(_.referralCode === code).exists.result)

  def isCodeOwnedBy(id: UUID, code: String): Future[Boolean] =
    db.run(dao.Query.filter(x => x.id === id && x.referralCode === code).exists.result)

  def getAccountByReferralCode(code: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(_.referralCode === code).result.headOption)

  def getByName(username: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.username === username).result.headOption)

  def getAccountByUserNamePassword(username: String, pass: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.username === username && x.password === pass).result.headOption)
  // if user has no referral get result else no result
  def hasNoReferralClaim(id: UUID): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.id === id && x.referredBy.isEmpty.?).result.headOption)

  def exist(username: String): Future[Boolean] =
    db.run(dao.Query(username).exists.result)

  def find(id: UUID, username: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(r => r.id === id && r.username === username)
      .result
      .headOption)

  def hasSession(username: String): Future[Boolean] =
    db.run(dao.Query.filter(x => x.username === username && x.token.isEmpty.?).exists.result)
  def hasValidSession(username: String, token: String, currentTime: Long): Future[Boolean] =
    db.run(dao.Query.filter(x =>
        x.username === username &&
        x.token === token &&
        x.tokenLimit >= currentTime
      ).exists.result)
  // and make sure token is valid by checking if not expired..
  def getBySessionToken(token: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.token === token && x.tokenLimit >= Instant.now.getEpochSecond).result.headOption)
  def getByNameAndSessionToken(username: String, token: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.username === username && x.token === token).result.headOption)

}
