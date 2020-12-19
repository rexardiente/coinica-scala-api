package akka.domain.common.objects

import akka.actor.ActorRef
import models.domain.eosio.{ TableRowsRequest, GQRowsResponse, GQCharacterData, GQCharacterPrevMatch }

case class Connect(connection: ActorRef) extends AnyVal
case class Disconnect(disconnection: ActorRef) extends AnyVal
case class VerifyGQUserTable(request: TableRowsRequest) extends AnyVal
case class SetOfGQCharacterInfo(list: Seq[GQCharacterData]) extends AnyVal
case class SetOfGQCharacterGameHistory(list: Seq[GQCharacterPrevMatch]) extends AnyVal
object BattleScheduler
object RemoveCharacterWithNoLife
// case class UpdateUserDB(data: TableRowsResponse)