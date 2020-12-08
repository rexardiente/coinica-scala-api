package models.domain.eosio

import play.api.libs.json._

object BinaryArgs {
  implicit def implicitData = Json.format[BinaryArgs]
}

case class BinaryArgs(binargs: JsValue)
