package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.VIPUser

@Singleton
class VIPUserRepo @Inject()(
    dao: models.dao.VIPUserDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(vip: VIPUser): Future[Int] =
    db.run(dao.Query += vip)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(vip: VIPUser): Future[Int] =
    db.run(dao.Query.filter(_.id === vip.id).update(vip))

  def all(): Future[Seq[VIPUser]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)

  def findByID(id: UUID): Future[Option[VIPUser]] =
    db.run(dao.Query(id).result.headOption)
}