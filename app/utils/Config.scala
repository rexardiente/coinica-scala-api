package utils

import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object Config {
	val config = ConfigFactory.load()
	val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = config.getInt("platform.default.system.scheduler")
	val GQ_DEFAULT_BATTLE_TIMER: Int = config.getInt("platform.games.GQ.battle.timer")
	val GQ: List[String] = config.getStringList("platform.games.contracts.ghostquest").asScala.toList
	val GQ_CODE: String = GQ(0)
	val GQ_TABLE: String = GQ(1)
	val GQ_SCOPE: String = GQ(2)

	val NODE_SERVER_URI: String = config.getString("eosio.eosjs.node.server.uri")
}