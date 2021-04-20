package models.repo

import java.util.UUID
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
  // if user has no referral get result else no result
  def hasNoReferralClaim(id: UUID): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.id === id && x.referredBy.isEmpty.?).result.headOption)

  def exist(username: String): Future[Boolean] =
    db.run(dao.Query(username).exists.result)

  def find(id: UUID, username: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(r => r.id === id && r.username === username)
      .result
      .headOption)
}
