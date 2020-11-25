package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{Transaction, Trace}

@Singleton
final class TransactionDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class TransactionTable(tag: Tag) extends Table[Transaction](tag, "TRANSACTION") {
    def id = column[UUID] ("ID")
    def traceID = column[String] ("TRACE_ID")
    def blockNum = column[Long] ("BLOCK_NUM")
    def blockTimestamp = column[Long] ("BLOCK_TIMESTAMP")
    def trace = column[Trace] ("TRACE")

   def * = (id, traceID, blockNum, blockTimestamp, trace) <> (Transaction.tupled, Transaction.unapply)
   // def pk = primaryKey("pk_a", (id, traceID))
   // def idx = index("idx_a", (id, traceID), unique = true)
  }

  object Query extends TableQuery(new TransactionTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(id: String) = this.withFilter(_.traceID === id)
  } 
}