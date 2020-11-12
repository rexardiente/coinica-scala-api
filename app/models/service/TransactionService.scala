package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{ JsValue, Json }
import models.domain.{ PaginatedResult, Transaction }
import models.repo.TransactionRepo

@Singleton
class TransactionService @Inject()(transactionRepo: TransactionRepo) extends utils.CommonImplicits {

  def paginatedResult[T >: Transaction](start: String, end: String, limit: Int, offset: Int): Future[JsValue] = {
  	val startToUnix: Option[Instant] = start
  	val endToUnix: Option[Instant] = end

		// check if start and end are valid Instant epocheSecond
  	try {
  		for {
	      txs <- transactionRepo.getByDate(
	      	startToUnix.get.getEpochSecond, 
	      	endToUnix.map(_.getEpochSecond).getOrElse(Instant.ofEpochSecond(startToUnix.get.getEpochSecond + 86400).getEpochSecond),  // Add 24hrs as default
	      	limit, 
	      	offset)
	      size <- transactionRepo.getSize()
	      hasNext <- Future(size - (offset + limit) > 0)
	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  	} catch {
  		case e: Throwable => Future(Json.obj("err" -> "Invalid UNIX date format")) // need some upgrades
  	}
  }
}