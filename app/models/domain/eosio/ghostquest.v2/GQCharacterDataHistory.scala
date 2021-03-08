package models.domain.eosio.GQ.v2

import play.api.libs.json._
import play.api.libs.functional.syntax._

object GQCharacterDataTrait extends utils.CommonImplicits
object GQCharacterData {
  def toCharacterDataHistory(v: GQCharacterData): GQCharacterDataHistory =
    new GQCharacterDataHistory(
      v.key,
      v.owner,
      v.life,
      v.hp,
      v.`class`,
      v.level,
      v.status,
      v.attack,
      v.defense,
      v.speed,
      v.luck,
      v.limit,
      v.count,
      v.isNew,
      v.createdAt)
}

object GQCharacterDataHistory {
  def toCharacterData(v: GQCharacterDataHistory): GQCharacterData =
    new GQCharacterData(
        v.key,
        v.owner,
        v.life,
        v.hp,
        v.`class`,
        v.level,
        v.status,
        v.attack,
        v.defense,
        v.speed,
        v.luck,
        v.limit,
        v.count,
        v.isNew,
        v.createdAt)
}
trait GQCharacterDataTrait {
    val key: String
    val owner: String
    // val prize: Double
    val `class`: Int
    val level: Int
    def toJson(): JsValue = Json.toJson(this)
}
case class GQCharacterData(
    key: String,
    owner: String,
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
    isNew: Boolean,
    createdAt: Long) extends GQCharacterDataTrait
case class GQCharacterDataHistory(
    key: String,
    owner: String,
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
    isNew: Boolean,
    createdAt: Long) extends GQCharacterDataTrait