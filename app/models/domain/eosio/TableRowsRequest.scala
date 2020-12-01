package models.domain.eosio

import play.api.libs.json._

object TableRowsRequest {
  implicit def implicitActionTrace = Json.format[TableRowsRequest]
}

case class TableRowsRequest(
    code: String, // Name of the smart contract that controls the provided table
    table: String, // Name of the table to query
    scope: String, // Account to which this data belongs
    index_position: Option[String], // Position of the index used, accepted parameters primary, secondary, tertiary, fourth, fifth, sixth, seventh, eighth, ninth , tenth
    key_type: Option[String], // Type of key specified by index_position (for example - uint64_t or name)
    encode_type: Option[String],
    upper_bound: Option[String],
    lower_bound: Option[String]) {
  def json: JsValue = Json.toJson(this)
}