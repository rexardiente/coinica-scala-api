package models.domain.eosio.GQ.v2

import play.api.libs.json._

object GQCharacterInfo extends utils.CommonImplicits
object GQGhost extends utils.CommonImplicits
object GQGame extends utils.CommonImplicits
object GQTable extends utils.CommonImplicits
object GQRowsResponse extends utils.CommonImplicits
case class GQCharacterInfo(
          life: Int,
          hp: Int,
          `class`: Int,
          level: Int,
          status: Int,
          attack: Int,
          defense: Int,
          speed: Int,
          luck: Int,
          limit: Int,
          count: Int,
          createdAt: Long)
case class GQGhost(key: String, value: GQCharacterInfo)
case class GQGame(characters: Seq[GQGhost])
case class GQTable(username: String, data: GQGame) {
  def toJson(): JsValue = Json.toJson(this)
}
case class GQRowsResponse(rows: Seq[GQTable], more: Boolean, next_key: String, sender: Option[String]) {
  def toJson(): JsValue = Json.toJson(this)
}
