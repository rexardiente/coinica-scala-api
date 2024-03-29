package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.wallet.support.UserAccountWalletHistory

@Singleton
class UserAccountWalletHistoryRepo @Inject()(
    dao: models.dao.UserAccountWalletHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def add(history: UserAccountWalletHistory): Future[Int] =
    db.run(dao.Query += history)
  def update(challenge: UserAccountWalletHistory): Future[Int] =
    db.run(dao.Query(challenge.id).update(challenge))
  def existByTxHash(txHash: String): Future[Boolean] =
    db.run(dao.Query(txHash).exists.result)
  def existByTxHashAndID(hash: String, id: UUID): Future[Boolean] =
    db.run(dao.Query.filter(x => x.txHash === hash && x.id === id).exists.result)
  def getByAccountID(id: UUID, limit: Int, offset: Int): Future[Seq[UserAccountWalletHistory]] =
    db.run(dao.Query(id).drop(offset).take(limit).result)
  def all(): Future[Seq[UserAccountWalletHistory]] =
    db.run(dao.Query.result)
  def getTotalSizeByID(id: UUID): Future[Int] =
    db.run(dao.Query(id).length.result)
  def size(): Future[Int] =
    db.run(dao.Query.length.result)
  // .map(_.size)
}