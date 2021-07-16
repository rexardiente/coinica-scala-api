package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.wallet.support.FailedCoinDeposit

@Singleton
class FailedCoinDepositRepo @Inject()(
    dao: models.dao.UserAccountDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def add(v: FailedCoinDeposit): Future[Int] =
    db.run(dao.FailedCoinDepositQuery += v)

  def delete(txHash: String): Future[Int] =
    db.run(dao.FailedCoinDepositQuery(txHash).delete)

  def getByID(id: UUID): Future[Seq[FailedCoinDeposit]] =
    db.run(dao.FailedCoinDepositQuery.filter(x => x.id === id).result)

  def isExists(txHash: String): Future[Boolean] =
    db.run(dao.FailedCoinDepositQuery(txHash).exists.result)
}