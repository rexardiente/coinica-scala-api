package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.GameTransactionHistory

@Singleton
class GameTransactionHistoryRepo @Inject()(
    dao: models.dao.GameTransactionHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  private val createAtFn = SimpleFunction.unary[Instant, Long]("CREATED_AT")

  def add(tx: GameTransactionHistory): Future[Int] = db.run(dao.Query += tx)
  def all(): Future[Seq[GameTransactionHistory]] = db.run(dao.Query.result)
  def delete(id: UUID): Future[Int] = db.run(dao.Query(id).delete)
  def update(tx: GameTransactionHistory): Future[Int] = db.run(dao.Query.filter(_.id === tx.id).update(tx))
  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)
  def getSize(): Future[Int] = db.run(dao.Query.length.result)

  def ++=(txs: Seq[GameTransactionHistory]): Future[Seq[Int]] = {
    for {
      hasExists <- Future.sequence(txs.map(x => db.run(dao.Query.filter(_.id === x.id).exists.result)))
      checker <- Future.successful(hasExists.filter(_ == true))
      toInsert <- {
        if (checker.isEmpty) Future.sequence(txs.map(v => add(v)))
        else Future(Seq.empty)
      }
    } yield (toInsert)
  }

  def findByID(id: UUID, limit: Int, offset: Int): Future[Seq[GameTransactionHistory]] =
    db.run(dao.Query.filter(r => r.id === id)
      .drop(offset)
      .take(limit)
      .result)

  def findByDateRange(start: Long, end : Long, limit: Int, offset: Int): Future[Seq[GameTransactionHistory]] =
    db.run(dao.Query.filter(r => createAtFn(r.createdAt) >= start && createAtFn(r.createdAt) <= end )
      .drop(offset)
      .take(limit)
      .result)
}