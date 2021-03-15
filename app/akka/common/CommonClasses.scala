package akka.common.objects

import java.util.UUID
import java.time.{ LocalTime, Instant }
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import models.domain.eosio.{ TableRowsRequest, GQBattleResult }
import models.domain.eosio.GQ.v2._

case class Connect(connection: ActorRef) extends AnyVal
case class Disconnect(disconnection: ActorRef) extends AnyVal
case class VerifyGQUserTable(request: TableRowsRequest, sender: Option[String])
case class OnUpdateGQList(request: String)
// case class SetOfGQCharacterInfo(list: Seq[GQCharacterData]) extends AnyVal
// case class SetOfGQCharacterGameHistory(list: Seq[GQCharacterPrevMatch]) extends AnyVal

object GQBattleScheduler {
	var nextBattle: Long = 0
  var battleStatus: String = "to_update" // "to_update", "on_update", "to_battle", "on_battle"
  val characters = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val eliminatedOrWithdrawn = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val isUpdatedCharacters = scala.collection.mutable.HashMap.empty[String, GQCharacterData]
  val battleCounter = scala.collection.mutable.HashMap.empty[UUID, GQBattleResult]
  val noEnemy = scala.collection.mutable.HashMap.empty[String, String] // character ID and username
  val smartcontractTxFailed = ListBuffer.empty[UUID] // character ID and username
  var isRemoving: Boolean = false
}
object RemoveCharacterWithNoLife
object GQResetScheduler
object GQSchedulerStatus
object ChallengeScheduler
case class ProcessOverAllChallenge(expiredAt: Long) extends AnyVal