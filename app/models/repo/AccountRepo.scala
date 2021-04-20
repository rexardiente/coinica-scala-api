package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Account

@Singleton
class AccountRepo @Inject()(
    dao: models.dao.AccountDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def add(v: Account): Future[Int] =
    db.run(dao.Query += v)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(v: Account): Future[Int] =
    db.run(dao.Query.filter(_.id === v.id).update(v))

  def isExist(username: String): Future[Boolean] =
    db.run(dao.Query.filter(_.username === username).exists.result)

  def isExist(username: String, password: String): Future[Boolean] =
    db.run(dao.Query.filter(x => x.username === username && x.password === password).exists.result)

  def findByUserName(username: String): Future[Option[Account]] =
    db.run(dao.Query.filter(_.username === username).result.headOption)

 }