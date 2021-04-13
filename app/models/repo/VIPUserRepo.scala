package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ VIPUser, VIPBenefit }

import scala.util.{ Success, Failure }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import models.dao._
import models.domain.enum._

@Singleton
class VIPUserRepo @Inject()(
    vipDAO: models.dao.VIPUserDAO,
    benefitDAO: models.dao.VIPBenefitDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  def add(vip: VIPUser): Future[Int] =
    db.run(vipDAO.Query += vip)

  def delete(id: UUID): Future[Int] =
    db.run(vipDAO.Query(id).delete)

  def update(vip: VIPUser): Future[Int] =
    db.run(vipDAO.Query.filter(_.id === vip.id).update(vip))

  def all(): Future[Seq[VIPUser]] =
    db.run(vipDAO.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(vipDAO.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[VIPUser]] =
    db.run(vipDAO.Query(id).result.headOption)

  def benefits(): Future[Seq[VIPBenefit]] =
    db.run(benefitDAO.Query.result)

  def addBenefit(benefit: VIPBenefit): Future[Int] =
    db.run(benefitDAO.Query += benefit)

  def updateBenefit(benefit: VIPBenefit): Future[Int] =
    db.run(benefitDAO.Query.filter(_.id === benefit.id).update(benefit))

  def getBenefitByAmount(amount: Double): Future[Seq[VIPBenefit]] =
    db.run(benefitDAO.Query(amount).result)

  def getBenefitByID(id: VIP.value): Future[Seq[VIPBenefit]] = {
    db.run(benefitDAO.Query(id).result)
  }
}