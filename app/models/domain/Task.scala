package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._

// limit is for how many times to play the game (25, 100, 500)
// durations: 1=1 day, 2=1week, 3=1month
object TaskHistory extends utils.CommonImplicits
object Task extends utils.CommonImplicits
object DailyTask extends utils.CommonImplicits {
	val tupled = (apply: (String, UUID, Int) => DailyTask).tupled
}
// case class TaskPerGame(game_id: UUID, game: String, limit: Int)
// before adding new Task if not user already exists else update
// Note: do not add if game hasnt exist in the list of tasks..
case class DailyTask(user: String, game_id: UUID, game_count: Int) {
	def toJson(): JsValue = Json.toJson(this)
}
// for getting history by week and month
// new UUID will be created so dont use this ID for matching..
case class TaskHistory(id: UUID,
											task_id: UUID,
											game_id: UUID,
											user: String,
											game_count: Int,
											created_at: Instant,
											expired_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}
// tasks are Seq[UUID] of game_id
case class Task(id: UUID, tasks: Seq[UUID], created_at: Instant) {
	// require(duration >= 1 || duration <= 3, "Invalid duration range")
	def toJson(): JsValue = Json.toJson(this)
}