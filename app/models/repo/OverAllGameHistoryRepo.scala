package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.OverAllGameHistory

@Singleton
class OverAllGameHistoryRepo @Inject()(
    dao: models.dao.OverAllGameHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  private val createAtFn = SimpleFunction.unary[Instant, Long]("CREATED_AT")

  def add(tx: OverAllGameHistory): Future[Int] = db.run(dao.Query += tx)
  def all(): Future[Seq[OverAllGameHistory]] = db.run(dao.Query.result)
  def all(limit: Int): Future[Seq[OverAllGameHistory]] = db.run(dao.Query.take(limit).result)
  def delete(id: UUID): Future[Int] = db.run(dao.Query(id).delete)
  def update(tx: OverAllGameHistory): Future[Int] = db.run(dao.Query.filter(_.id === tx.id).update(tx))
  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)
  def getSize(): Future[Int] = db.run(dao.Query.length.result)

  def ++=(txs: Seq[OverAllGameHistory]): Future[Seq[Int]] = {
    for {
      hasExists <- Future.sequence(txs.map(x => db.run(dao.Query.filter(_.id === x.id).exists.result)))
      checker <- Future.successful(hasExists.filter(_ == true))
      toInsert <- {
        if (checker.isEmpty) Future.sequence(txs.map(v => add(v)))
        else Future(Seq.empty)
      }
    } yield (toInsert)
  }

  def findByID(id: UUID, limit: Int, offset: Int): Future[Seq[OverAllGameHistory]] =
    db.run(dao.Query.filter(r => r.id === id)
      .drop(offset)
      .take(limit)
      .result)

  def getByDateRange(start: Instant, end : Instant): Future[Seq[OverAllGameHistory]] =
    db.run(dao.Query.filter(r => r.createdAt >= start && r.createdAt <= end ).result)
}