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

  def add(name: UserAccount): Future[Int] =
    db.run(dao.Query += name)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(name: UserAccount): Future[Int] =
    db.run(dao.Query.filter(_.id === name.id).update(name))

  def all(): Future[Seq[UserAccount]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def getByID(id: UUID): Future[Option[UserAccount]] =
    db.run(dao.Query(id).result.headOption)

  def isCodeExist(code: String): Future[Boolean] =
    db.run(dao.Query.filter(_.referralCode === code).exists.result)

  def isCodeOwnedBy(id: UUID, code: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.id === id && x.referralCode === code).result.headOption)

  def getAccountByReferralCode(code: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(_.referralCode === code).result.headOption)

  def getByName(name: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.name === name).result.headOption)
  // if user has no referral get result else no result
  def hasNoReferral(id: UUID): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(x => x.id === id && x.referredBy.isEmpty.?).result.headOption)

  def exist(name: String): Future[Boolean] =
    db.run(dao.Query(name).exists.result)

  def find(id: UUID, name: String): Future[Option[UserAccount]] =
    db.run(dao.Query.filter(r => r.id === id && r.name === name)
      .result
      .headOption)

  def getUserAccount(name: String): Future[Option[UserAccount]] =
    db.run(dao.Query(name).result.headOption)
}
