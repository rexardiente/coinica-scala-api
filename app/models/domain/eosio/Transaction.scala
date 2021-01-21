package models.domain.eosio

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Data extends utils.CommonImplicits 
object Receipt extends utils.CommonImplicits 
object Act extends utils.CommonImplicits 
object Partial extends utils.CommonImplicits 
object ActionTrace extends utils.CommonImplicits
object Trace extends utils.CommonImplicits
object Transaction extends utils.CommonImplicits { val tupled = (apply _).tupled }
case class Data(
		from: String,
		to: String,
		quantity: String,
		memo: Option[String])
case class Receipt(
		receiver: String,
		act_digest: String,
		global_sequence: Long,
		recv_sequence: Int,
		auth_sequence: JsArray,
		code_sequence: Int,
		abi_sequence: Int)
case class Act(
		account: String,
		name: String,
		authorization: JsArray,
		data: Data)
case class Partial(
		expiration: Option[Instant],
		ref_block_num: Long,
		ref_block_prefix: Long,
		max_net_usage_words: Int,
		max_cpu_usage_ms: Int,
		delay_sec: Int,
		transaction_extensions: JsValue,
		signatures: Seq[String],
		context_free_data: JsValue)
case class ActionTrace(
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
		error_code: Option[String])
case class Trace(
		id: String,
		status: String,
		cpuUsageUs: Int,
		netUsageWords: Int,
		elapsed: Int,
		netUsage: Int,
		scheduled: Boolean,
		actTraces: Seq[ActionTrace],
		accRamDelta: Option[String],
		except: Option[String],
		errorCode: Option[String],
		failedDtrxTrace: JsValue,
		partial: Option[Partial])
case class Transaction(
		id: UUID,
		traceId: String,
		blockNum: Long,
		blockTimestamp: Long, // Set to PK
		trace: Trace) { def toJson() = Json.toJson(this) }