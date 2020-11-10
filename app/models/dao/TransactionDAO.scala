package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.domain.{Transaction, Trace, Partial, Act, Data, Receipt}

@Singleton
final class TransactionDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.Implicits {
  import profile.api._

  protected class TransactionTable(tag: Tag) extends Table[Transaction](tag, "TRANSACTION") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def txID = column[String] ("TX_ID")
    def status = column[String] ("STATUS")
    def cpuUsageUs = column[Int] ("CPU_USAGE_US")
    def netUsageWords = column[Int] ("NET_USAGE_WORDS")
    def elapsed = column[Int] ("ELAPSED")
    def netUsage = column[Int] ("NET_USAGE")
    def scheduled = column[Boolean] ("SCHEDULED")
    def actTraces = column[Seq[Trace]] ("ACT_TRACES")
    def accRamDelta = column[String] ("ACC_RAM_DELTA")
    def except = column[String] ("EXCEPT")
    def errCode = column[String] ("ERR_CODE")
    def failedDtrxTrace = column[JsValue] ("FAILED_DTRX_TRACE")
    def partial = column[Partial] ("PARTIAL")
    def date = column[Instant] ("DATE") 

   def * = (id,
            txID,
            status,
            cpuUsageUs,
            netUsageWords,
            elapsed,
            netUsage,
            scheduled,
            actTraces,
            accRamDelta.?,
            except.?,
            errCode.?,
            failedDtrxTrace,
            partial.?,
            date) <> (Transaction.tupled, Transaction.unapply)

  }

  object Query extends TableQuery(new TransactionTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(id: String) = this.withFilter(_.txID === id)
  } 
}