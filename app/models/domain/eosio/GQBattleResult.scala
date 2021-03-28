package models.domain.eosio

import java.util.UUID
import play.api.libs.json._

object GameLog {
  implicit def implicitGameLog = Json.format[GameLog]
}
object GQBattleResult {
  implicit def implicitGQBattleResult = Json.format[GQBattleResult]
}
case class GameLog(round: Int, attacker: UUID, defender: UUID, damage: Int, is_crit: Boolean)
case class GQBattleResult(id: UUID, characters: Map[String, (UUID, Boolean)], logs: List[GameLog])