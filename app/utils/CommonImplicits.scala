package utils

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import models.domain._

trait CommonImplicits {
	// Common
	implicit def dateTimeStringToUnix(s: String): Option[Instant] =
    try {
      Some(Instant.parse(s))
    } catch {
      case _: Throwable => None
    }

	// Models domain
	implicit def implGame = Json.format[Game]
	implicit def implGenre = Json.format[Genre]
	implicit def implicitData = Json.format[Data]
	implicit def implicitReceipt = Json.format[Receipt]
	implicit def implicitAct = Json.format[Act]
	implicit def implicitPartial = Json.format[Partial]
	implicit def implicitActionTrace = Json.format[ActionTrace]

	// Models Custom Validations
	implicit val implicitTraceReads: Reads[Trace] = new Reads[Trace] {
		override def reads(js: JsValue): JsResult[Trace] = js match {
			case json: JsValue => {
				try {
					JsSuccess(Trace(
						(json \ "id").as[String],
						(json \ "status").as[String],
						(json \ "cpu_usage_us").as[Int],
						(json \ "net_usage_words").as[Int],
						(json \ "elapsed").as[Int],
						(json \ "net_usage").as[Int],
						(json \ "scheduled").as[Boolean],
						(json \ "action_traces").as[Seq[ActionTrace]],
						(json \ "account_ram_delta").asOpt[String],
						(json \ "except").asOpt[String],
						(json \ "error_code").asOpt[String],
						(json \ "failed_dtrx_trace").as[JsValue],
						(json \ "partial").asOpt[Partial]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitTraceWrites = new Writes[Trace] {
	  def writes(tx: Trace) = Json.obj(
		"id" -> tx.id,
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
		"partial" -> tx.partial)
	}
	implicit val implicitTxReads: Reads[Transaction] = new Reads[Transaction] {
		override def reads(js: JsValue): JsResult[Transaction] = js match {
			case json: JsValue => {
				try {
					JsSuccess(Transaction(
						(json \ "id").as[UUID],
						(json \ "trace_id").as[String],
						(json \ "block_num").as[Long],
						(json \ "block_timestamp").as[Long],
					 	(json \ "trace").as[Trace]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitTxWrites = new Writes[Transaction] {
	  def writes(tx: Transaction) = Json.obj(
		"id" -> tx.id,
		"trace_id" -> tx.traceId,
		"block_num" -> tx.blockNum,
		"block_timestamp" -> Instant.ofEpochSecond(tx.blockTimestamp),
		"trace" -> tx.trace)
	}
	implicit val implicitInEventReads: Reads[InEvent] = new Reads[InEvent] {
		override def reads(js: JsValue): JsResult[InEvent] = js match {
			case json: JsValue => {
				try {
					JsSuccess(InEvent((json \ "id").as[JsString], (json \ "input").as[JsValue]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitInEventWrites = new Writes[InEvent] {
	  def writes(tx: InEvent) = {
	  	if (tx.id == JsNull) 
	  		Json.obj("input" -> tx.input)
	  	else
	  		Json.obj("id" -> tx.id, "input" -> tx.input)
	  }
	}
  	implicit val implicitOutEventReads: Reads[OutEvent] = new Reads[OutEvent] {
		override def reads(js: JsValue): JsResult[OutEvent] = js match {
			case json: JsValue => {
				try {
					JsSuccess(OutEvent((json \ "id").as[JsString], (json \ "response").as[JsValue]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitOutEventWrites = new Writes[OutEvent] {
	  def writes(tx: OutEvent) = {
	  	if (tx.id == JsNull) 
	  		Json.obj("response" -> tx.response)
	  	else
	  		Json.obj("id" -> tx.id, "response" -> tx.response)
	  }
	}

	// EOSIO Tables..
	implicit def implGQGhost = Json.format[models.domain.eosio.GQGhost]
	implicit def implGQGame = Json.format[models.domain.eosio.GQGame]
	implicit def implGQTable = Json.format[models.domain.eosio.GQTable]
	implicit def implTableRowsResponse = Json.format[models.domain.eosio.TableRowsResponse]
}