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
	implicit def implicitTransaction = Json.format[Transaction]
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
		tx_id: String,
		status: String,
		cpu_usage_us: Int,
		net_usage_words: Int,
		elapsed: Int,
		net_usage: Int,
		scheduled: Boolean,
		action_traces: Seq[Trace],
		account_ram_delta: Option[String],
		except: Option[String],
		error_code: Option[String],
		failed_dtrx_trace: JsValue,
		partial: Option[Partial])  { def toJson() = Json.toJson(this) }