package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Login

@Singleton
class LoginRepo @Inject()(
    dao: models.dao.LoginDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  def add(login: Login): Future[Int] =
    db.run(dao.Query += login)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(login: Login): Future[Int] =
    db.run(dao.Query.filter(_.id ===login.id).update(login))

  }