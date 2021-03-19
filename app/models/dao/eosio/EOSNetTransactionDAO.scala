package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ EOSNetTransaction, Trace }

@Singleton
final class EOSNetTransactionDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class EOSNetTransactionTable(tag: Tag) extends Table[EOSNetTransaction](tag, "EOS_NET_TRANSACTION") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def traceID = column[String] ("TRACE_ID")
    def blockNum = column[Long] ("BLOCK_NUM")
    def blockTimestamp = column[Long] ("BLOCK_TIMESTAMP")
    def trace = column[Trace] ("TRACE")

    def * = (id, traceID, blockNum, blockTimestamp, trace) <> (EOSNetTransaction.tupled, EOSNetTransaction.unapply)
  }

  object Query extends TableQuery(new EOSNetTransactionTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(id: String) = this.withFilter(_.traceID === id)
  }
}