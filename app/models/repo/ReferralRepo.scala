package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Referral

@Singleton
class ReferralRepo @Inject()(
    dao: models.dao.ReferralDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(referral: Referral): Future[Int] =
    db.run(dao.Query += referral)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(referral: Referral): Future[Int] =
    db.run(dao.Query.filter(_.id ===referral.id).update(referral))

  def all(): Future[Seq[Referral]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def getByCode(code: String): Future[Seq[Referral]] =
    db.run(dao.Query.filter(r => r.code === code).result)

  // def findByDaily(currentdate: Long, limit: Int, offset: Int): Future[Seq[Referral]] =
  //  db.run(dao.Query.filter(r =>  r.createdAt === currentdate)
  //    .drop(offset)
  //    .take(limit)
  //    .result)
  // def findByDateRange(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Seq[Referral]] =
  //  db.run(dao.Query.filter(r => r.createdAt >= startdate && r.createdAt <= enddate )
  //    .drop(offset)
  //    .take(limit)
  //    .result)

  def findAll(limit: Int, offset: Int): Future[Seq[Referral]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)


}