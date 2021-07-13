package models.service

import javax.inject.{ Inject, Singleton }
import java.time.{ LocalTime, LocalDate, LocalDateTime, Instant, ZoneId, ZoneOffset }
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain._
import models.repo._

@Singleton
class HistoryService @Inject()(gameTxHistory: OverAllGameHistoryRepo) {
	def getOverAllHistory(): Future[Seq[OverAllGameHistory]] = gameTxHistory.all
	def getOverAllHistorySize(): Future[Int] = gameTxHistory.getSize()
	def getHistoryByGameName(v: String): Future[Seq[OverAllGameHistory]] = gameTxHistory.getByGameName(v: String)

}