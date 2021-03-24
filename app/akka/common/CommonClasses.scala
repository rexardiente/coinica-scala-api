package akka.common.objects

import java.util.UUID
import java.time.{ LocalTime, Instant }
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import models.domain.eosio.{ TableRowsRequest, GQBattleResult }
import models.domain.eosio.GQ.v2._

case class Connect(username: String) extends AnyVal
// case class Disconnect(disconnection: ActorRef) extends AnyVal
case class VerifyGQUserTable(request: TableRowsRequest, sender: Option[String])
case class OnUpdateGQList(request: String) extends AnyVal
case class ProcessOverAllChallenge(expiredAt: Long) extends AnyVal

object REQUEST_BATTLE_NOW
case class REQUEST_TABLE_ROWS(req: TableRowsRequest, sender: Option[String])
object REQUEST_CHARACTER_ELIMINATE
object REQUEST_UPDATE_CHARACTERS_DB
object GQBattleScheduler {
	var nextBattle: Long = 0
  // var battleStatus: String = "to_update" // "to_update", "on_update", "to_battle", "on_battle"
  val characters = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val eliminatedOrWithdrawn = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val isUpdatedCharacters = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val battleCounter = scala.collection.mutable.HashMap.empty[UUID, GQBattleResult]
  val noEnemy = scala.collection.mutable.HashMap.empty[String, String] // character ID and username
  // val smartcontractTxFailed = ListBuffer.empty[UUID] // character ID and username
  // var isRemoving: Boolean = false
  var REQUEST_BATTLE_STATUS = ""
}
// object RemoveCharacterWithNoLife
// object GQResetScheduler
// object GQSchedulerStatus
object ChallengeScheduler
object DailyTaskScheduler
object CreateNewDailyTask
object RankingScheduler