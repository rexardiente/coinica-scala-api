package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Data {
	implicit def implicitData = Json.format[Data]
}
object Receipt {
	implicit def implicitReceipt = Json.format[Receipt]
}
object Act {
	implicit def implicitAct = Json.format[Act]
}
object Partial {
	implicit def implicitPartial = Json.format[Partial]
}
object Trace {
	implicit def implicitTrace = Json.format[Trace]
}
object Transaction {
	val tupled = (apply _).tupled

	implicit val findUserReads: Reads[Transaction] = new Reads[Transaction] {
		override def reads(js: JsValue): JsResult[Transaction] = js match {
			case json: JsValue => {
				try {
					JsSuccess(Transaction(
						(json \ "id").as[UUID],
						(json \ "tx_id").as[String],
						(json \ "status").as[String],
						(json \ "cpu_usage_us").as[Int],
						(json \ "net_usage_words").as[Int],
						(json \ "elapsed").as[Int],
						(json \ "net_usage").as[Int],
						(json \ "scheduled").as[Boolean],
						(json \ "action_traces").as[Seq[Trace]],
						(json \ "account_ram_delta").asOpt[String],
						(json \ "except").asOpt[String],
						(json \ "error_code").asOpt[String],
						(json \ "failed_dtrx_trace").as[JsValue],
						(json \ "partial").asOpt[Partial],
					 	Instant.ofEpochSecond((json \ "date").as[Long])))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}

	implicit val locationWrites = new Writes[Transaction] {
	  def writes(tx: Transaction) = Json.obj(
	    "id" -> tx.id,
		"tx_id" -> tx.txId,
		"status" -> tx.status,
		"cpu_usage_us" -> tx.cpuUsageUs,
		"net_usage_words" -> tx.netUsageWords,
		"elapsed" -> tx.elapsed,
		"net_usage" -> tx.netUsage,
		"scheduled" -> tx.scheduled,
		"action_traces" -> tx.actTraces,
		"account_ram_delta" -> tx.accRamDelta,
		"except" -> tx.except,
		"error_code" -> tx.errorCode,
		"failed_dtrx_trace" -> tx.failedDtrxTrace,
		"partial" -> tx.partial,
	 	"date" -> tx.date)
	}
}

case class Data(
		from: String,
		to: String,
		quantity: String,
		memo: Option[String]) { def toJson() = Json.toJson(this) }
case class Receipt(
		receiver: String,
		act_digest: String,
		global_sequence: Int,
		recv_sequence: Int,
		auth_sequence: JsArray,
		code_sequence: Int,
		abi_sequence: Int) { def toJson() = Json.toJson(this) }
case class Act(
		account: String,
		name: String,
		authorization: JsArray,
		data: Data) { def toJson() = Json.toJson(this) }
case class Partial(
		expiration: Option[Instant],
		ref_block_num: Long,
		ref_block_prefix: Long,
		max_net_usage_words: Int,
		max_cpu_usage_ms: Int,
		delay_sec: Int,
		transaction_extensions: JsValue,
		signatures: Seq[String],
		context_free_data: JsValue) { def toJson() = Json.toJson(this) }
case class Trace(
		action_ordinal: Int,
		creator_action_ordinal: Int,
		receipt: Receipt,
		receiver: String,
		act: JsValue,
		context_free: Boolean,
		elapsed: Int,
		console: Option[String],
		account_ram_deltas: Option[JsValue],
		except: Option[String],
		error_code: Option[String]) { def toJson() = Json.toJson(this) }
case class Transaction(
		id: UUID,
		txId: String,
		status: String,
		cpuUsageUs: Int,
		netUsageWords: Int,
		elapsed: Int,
		netUsage: Int,
		scheduled: Boolean,
		actTraces: Seq[Trace],
		accRamDelta: Option[String],
		except: Option[String],
		errorCode: Option[String],
		failedDtrxTrace: JsValue,
		partial: Option[Partial],
		date: Instant)  { def toJson() = Json.toJson(this) }