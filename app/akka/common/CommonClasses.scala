package akka.common.objects

import java.util.UUID
import java.time.{ LocalTime, Instant }
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.actor.ActorRef
import models.domain.eosio.{ TableRowsRequest, GQBattleResult }
import models.domain.eosio.GQ.v2._

case class Connect(username: String) extends AnyVal
// case class Disconnect(disconnection: ActorRef) extends AnyVal
case class VerifyGQUserTable(request: TableRowsRequest, sender: Option[String])
case class OnUpdateGQList(request: String) extends AnyVal
case class REQUEST_TABLE_ROWS(req: TableRowsRequest, sender: Option[String])

object REQUEST_BATTLE_NOW
object REQUEST_CHARACTER_ELIMINATE
object REQUEST_UPDATE_CHARACTERS_DB
object ChallengeScheduler
object DailyTaskScheduler
object CreateNewDailyTask
object RankingScheduler
object GQBattleScheduler {
	var nextBattle: Long = 0
  val characters = HashMap.empty[String, GQCharacterData]
  val eliminatedOrWithdrawn = HashMap.empty[String, GQCharacterData]
  val isUpdatedCharacters = HashMap.empty[String, GQCharacterData]
  val toRemovedCharacters = HashMap.empty[String, GQCharacterData] // character ID and user
  val battleCounter = HashMap.empty[UUID, GQBattleResult]
  val noEnemy = HashMap.empty[String, UUID] // character ID and username
}