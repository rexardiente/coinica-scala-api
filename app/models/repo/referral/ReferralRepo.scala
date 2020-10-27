package models.repo.referral

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.referral.Referral

@Singleton
class ReferralRepo @Inject()(
    dao: models.dao.referral.ReferralDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(referral: Referral): Future[Int] =
    db.run(dao.Query += referral)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)
 
  def update(referral: Referral): Future[Int] =
    db.run(dao.Query.filter(_.id === referral.id).update(referral))


  def all(): Future[Seq[Referral]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[Referral]] =
    db.run(dao.Query.filter(r => r.id === id)
      .result
      .headOption)

  def findByName(name: String): Future[Option[Referral]] =
    db.run(dao.Query.filter(r => r.name === name)
      .result
      .headOption)
}
