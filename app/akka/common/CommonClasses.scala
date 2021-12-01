package akka.common.objects

import models.domain.eosio.TableRowsRequest

case class Connect(username: String) extends AnyVal
case class VerifyGQUserTable(request: TableRowsRequest, sender: Option[String])
case class OnUpdateGQList(request: String) extends AnyVal
case class REQUEST_TABLE_ROWS(req: TableRowsRequest, sender: Option[String])

object REQUEST_BATTLE_NOW
object REQUEST_CHARACTER_ELIMINATE
object REQUEST_UPDATE_CHARACTERS_DB
object ChallengeScheduler
object DailyTaskScheduler
object CreateNewDailyTask
case class RankingScheduler(val data: Seq[models.domain.ChallengeTracker]) extends AnyVal