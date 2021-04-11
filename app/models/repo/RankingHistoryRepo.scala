package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.RankingHistory

@Singleton
class RankingHistoryRepo @Inject()(
    dao: models.dao.RankingHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(ranking: RankingHistory): Future[Int] =
    db.run(dao.Query += ranking)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(ranking: RankingHistory): Future[Int] =
    db.run(dao.Query.filter(_.id === ranking.id).update(ranking))

  def all(limit: Int, offset: Int): Future[Seq[RankingHistory]] =
    db.run(dao.Query
      .drop(offset)
      .take(limit)
      .result)

  def findByDateRange(date: Long): Future[Option[RankingHistory]] =
   db.run(dao.Query.filter(r => r.createdAt >= date && r.createdAt <= date).result.headOption)

  def findAll(limit: Int, offset: Int): Future[Seq[RankingHistory]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)

  def getHistoryByDateRange(start: Long, end: Long): Future[Seq[RankingHistory]] = {
    db.run(dao.Query.filter(r => r.createdAt >= start && r.createdAt <= end).result)
    // db.run(dao.Query.sortBy(_.createdAt.desc).take(limit).result)
  }
}