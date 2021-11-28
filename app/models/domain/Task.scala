package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import utils.CommonImplicits

// limit is for how many times to play the game (25, 100, 500)
// durations: 1=1 day, 2=1week, 3=1month
object TaskHistory extends CommonImplicits
object TaskGameInfo extends CommonImplicits
object Task extends CommonImplicits
object DailyTask extends CommonImplicits {
	val tupled = (apply: (UUID, UUID, UUID, Int) => DailyTask).tupled
}
// case class TaskPerGame(game_id: UUID, game: String, limit: Int)
// before adding new Task if not user already exists else update
// Note: do not add if game hasnt exist in the list of tasks..
case class DailyTask(id: UUID, user: UUID, game_id: UUID, game_count: Int) {
	def toJson(): JsValue = Json.toJson(this)
}
// for getting history by week and month
// new UUID will be created so dont use this ID for matching..
case class TaskHistory(id: UUID,
											tasks: List[DailyTask],
											valid_at: Instant,
											expired_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}
// points to be generated will be 0.1 to 2 VIP points
// progress will only be used on UI
case class TaskGameInfo(game: PlatformGame, count: Int, points: Double, progress: Option[Int])
// tasks are Seq[(game_id, game_count_required)]
case class Task(id: UUID, tasks: Seq[TaskGameInfo], created_at: Long) {
	def toJson(): JsValue = Json.toJson(this)
}