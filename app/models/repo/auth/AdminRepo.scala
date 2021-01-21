package models.repo

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import cats.data.OptionT
import cats.implicits._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ Admin, Security }
import models.dao.AdminDAO

@Singleton
class AdminRepo @Inject()(
    dao: AdminDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def find(id: UUID): OptionT[Future, (Admin, Security)] =
      OptionT(db.run(dao.Query(id).result.headOption))
}
