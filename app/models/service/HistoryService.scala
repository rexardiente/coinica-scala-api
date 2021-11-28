package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Ordering.Double.IeeeOrdering
import play.api.libs.json._
import models.domain._
import models.repo._

@Singleton
class HistoryService @Inject()(overallGameHistory: OverAllGameHistoryRepo) {
	def getOverAllHistory(): Future[Seq[OverAllGameHistory]] = overallGameHistory.all
	def getOverAllHistorySize(): Future[Int] = overallGameHistory.getSize()
	def getHistoryByGameName(v: String): Future[Seq[OverAllGameHistory]] = overallGameHistory.getByGameName(v: String)
	def addOverAllHistory(v: OverAllGameHistory): Future[Int] = overallGameHistory.add(v)



}