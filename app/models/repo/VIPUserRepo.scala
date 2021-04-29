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
import models.domain.enum._

@Singleton
class VIPUserRepo @Inject()(
    userAccDAO: models.dao.UserAccountDAO,
    benefitDAO: models.dao.VIPBenefitDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  def add(vip: VIPUser): Future[Int] =
    db.run(userAccDAO.VIPUserQuery += vip)

  def delete(id: UUID): Future[Int] =
    db.run(userAccDAO.VIPUserQuery(id).delete)

  def update(vip: VIPUser): Future[Int] =
    db.run(userAccDAO.VIPUserQuery.filter(_.id === vip.id).update(vip))

  def all(): Future[Seq[VIPUser]] =
    db.run(userAccDAO.VIPUserQuery.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(userAccDAO.VIPUserQuery(id).exists.result)

  def findByID(id: UUID): Future[Option[VIPUser]] =
    db.run(userAccDAO.VIPUserQuery(id).result.headOption)

  def benefits(): Future[Seq[VIPBenefit]] =
    db.run(benefitDAO.Query.result)

  def addBenefit(benefit: VIPBenefit): Future[Int] =
    db.run(benefitDAO.Query += benefit)

  def updateBenefit(benefit: VIPBenefit): Future[Int] =
    db.run(benefitDAO.Query.filter(_.id === benefit.id).update(benefit))

  def getBenefitByAmount(amount: Double): Future[Option[VIPBenefit]] =
    db.run(benefitDAO.Query(amount).result.headOption)

  def getBenefitByID(id: VIP.value): Future[Option[VIPBenefit]] = {
    db.run(benefitDAO.Query(id).result.headOption)
  }
}