package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ReferralHistory

@Singleton
class ReferralHistoryRepo @Inject()(
    dao: models.dao.ReferralHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(referralHistory: ReferralHistory): Future[Int] =
    db.run(dao.Query += referralHistory)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(referralHistory: ReferralHistory): Future[Int] =
    db.run(dao.Query.filter(_.id ===referralHistory.id).update(referralHistory))

  def all(): Future[Seq[ReferralHistory]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def getByCode(code: String): Future[Seq[ReferralHistory]] =
    db.run(dao.Query.filter(r => r.code === code).result)

  def isReferred(appliedBy: UUID, code: String): Future[Boolean] =
    db.run(dao.Query.filter(r => r.appliedBy === appliedBy && r.code === code).exists.result)

  // def isReferrer(referrer: UUID, referred: String): Future[Boolean] = ???
  //   db.run(dao.Query.filter(r => r.appliedBy === referrer && r.code === referred).exists.result)

  // def findByDaily(currentdate: Long, limit: Int, offset: Int): Future[Seq[ReferralHistory]] =
  //  db.run(dao.Query.filter(r =>  r.createdAt === currentdate)
  //    .drop(offset)
  //    .take(limit)
  //    .result)
  // def findByDateRange(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Seq[ReferralHistory]] =
  //  db.run(dao.Query.filter(r => r.createdAt >= startdate && r.createdAt <= enddate )
  //    .drop(offset)
  //    .take(limit)
  //    .result)

  def findAll(limit: Int, offset: Int): Future[Seq[ReferralHistory]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)


}