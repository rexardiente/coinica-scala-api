package models.repo

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Transaction

@Singleton
class TransactionRepo @Inject()(
    dao: models.dao.TransactionDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(tx: Transaction): Future[Int] =
    db.run(dao.Query += tx)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(tx: Transaction): Future[Int] =
    db.run(dao.Query.filter(_.id === tx.id).update(tx))

  def all(): Future[Seq[Transaction]] =
    db.run(dao.Query.result)

  def exist(txID: String): Future[Boolean] = 
    db.run(dao.Query(txID).exists.result)

  def findByID(id: UUID): Future[Option[Transaction]] =
    db.run(dao.Query(id).result.headOption)

  def findTxID(id: String): Future[Option[Transaction]] = {
    db.run(dao.Query(id).result.headOption)
  }

  def getByDate(start: Instant, end: Instant): Future[Seq[Transaction]] = {
    db.run(dao.Query.filter(d => d.blockTimestamp >= start && d.blockTimestamp <= end).result) 
  }
}
