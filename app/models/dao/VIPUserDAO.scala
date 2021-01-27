package models.dao

import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import ejisan.scalauthx.HashedCredential
import models.domain.VIPUser
import models.domain.enum._

@Singleton
final class VIPUserDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class VIPUserTable(tag: Tag) extends Table[VIPUser](tag, "VIP") {
    def user = column[String] ("USER", O.PrimaryKey)
    def rank = column[VIP.value] ("RANK")
    def nxtRank = column[VIP.value] ("NEXT_RANK")
    def payout = column[Long] ("PAYOUT")
    def points = column[Long] ("POINTS")
    def nextLvl = column[Int] ("NEXT_LEVEL")
    def updatedAt = column[Instant] ("UPDATED_AT")

    def * = (user, rank, nxtRank, payout, points, nextLvl, updatedAt) <> (VIPUser.tupled, VIPUser.unapply)
  }

  object Query extends TableQuery(new VIPUserTable(_)) {
    def apply(user: String) = this.withFilter(_.user === user)
  }
}