package models.repo

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.UserAccountWallet

import models.domain.wallet.support.{ Coin, CoinDeposit }
@Singleton
class UserAccountWalletRepo @Inject()(
    dao: models.dao.UserAccountDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  def add(user: UserAccountWallet): Future[Int] =
    db.run(dao.UserWalletQuery += user)
  def delete(id: UUID): Future[Int] =
    db.run(dao.UserWalletQuery(id).delete)
  def update(user: UserAccountWallet): Future[Int] =
    db.run(dao.UserWalletQuery.filter(_.id === user.id).update(user))
  def getByID(id: UUID): Future[Option[UserAccountWallet]] =
    db.run(dao.UserWalletQuery(id).result.headOption)
  def exists(id: UUID): Future[Boolean] =
    db.run(dao.UserWalletQuery(id).exists.result)
}
