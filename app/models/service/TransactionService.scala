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

  def paginatedResult[T >: Transaction](start: Instant, end: Option[Instant], limit: Int, offset: Int): Future[JsValue] = {
		// check if start and end are valid Instant epocheSecond
  	try {
  		for {
	      txs <- transactionRepo.getByDate(
	      	start.getEpochSecond, 
	      	end.map(_.getEpochSecond).getOrElse(start.getEpochSecond),  // Add 24hrs as default Instant.ofEpochSecond(start.getEpochSecond + 86400).getEpochSecond
	      	limit, 
	      	offset)
	      size <- transactionRepo.getSize()
	      hasNext <- Future(size - (offset + limit) > 0)
	    } yield Json.toJson(PaginatedResult(txs.size, txs.toList, hasNext))
  	} catch {
  		case e: Throwable => Future(Json.obj("err" -> e.toString)) // need some upgrades
  	}
  }
}