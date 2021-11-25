package utils.db.schema

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.json._
import models.dao.VIPBenefitDAO
import models.repo._
import models.domain.enum._
import models.domain._

import utils.GameConfig._

@Singleton
class DBDefaultGenerator @Inject()(
    dao: VIPBenefitDAO,
    gameRepo: GameRepo,
    genreRepo: GenreRepo,
    configRepo: PlatformConfigRepo,
    val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._
  // default variables
  private val defaultGenre: Genre = new Genre(generateID, "LUCKY", Some("Play with your luck."))
  // queries here...
  private def generateID(): UUID = UUID.randomUUID()
  private def vipBenefitQuery(): Future[Seq[Int]] = {
    val querySet = Seq(new VIPBenefit(VIP.BRONZE, 1.0, .10, 0.12, false, false, VIPBenefitAmount.BRONZE, VIPBenefitPoints.BRONZE, Instant.now()),
                      new VIPBenefit(VIP.SILVER, 3.0, .20, 0.14, false, false, VIPBenefitAmount.SILVER, VIPBenefitPoints.SILVER, Instant.now()),
                      new VIPBenefit(VIP.GOLD, 5.0, .30, 0.16, false, true, VIPBenefitAmount.GOLD, VIPBenefitPoints.GOLD, Instant.now()))

    Future.sequence(querySet.map(x => db.run(dao.Query += x)))
  }
  private def genreQuery(): Future[Int] = genreRepo.add(defaultGenre)
  private def systemDefaultConfigQuery(): Future[Int] = {
    val GQ_DESCRIPTION: String = """Idle game with character-battle. You need only to draw loot box and summon ghosts and send them in battle field. If your ghost wins, you can get rewrard."""
    val TH_DESCRIPTION: String = """Seeking for treasure in three different ocean maps without being spotted by rival pirates!"""
    val MJ_DESCRIPTION: String = """High-low game with mahjong tiles, if you can complete a hand with 14 tiles until 33rd turn, you get a bonus according to your hand score ranking."""
    // ghostquest = ["ghostquest", "0f335579-1bf8-4f9e-8ede-eb204f5c0cba", "GQ"]
    // treasurehunt = ["treasurehunt", "1b977a2b-842e-430b-bd1b-c0bd3abe1c55", "TH"]
    // mahjonghilo = ["mahjonghilo", "74cd374c-6126-495a-a8a3-33db87caa511", "MJ"]
    // GQ.battle.timer = 15 will be move in others
    val games = List(new PlatformGame("ghostquest",
                                      "GHOST QUEST",
                                      "GQ",
                                      "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/GQ.jpeg",
                                      "/game/ghostquest",
                                      defaultGenre.id,
                                      JsNull,
                                      GQ_DESCRIPTION),
                    new PlatformGame("treasurehunt",
                                      "TREASURE HUNT",
                                      "TH",
                                      "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/TH.jpeg",
                                      "/game/treasurehunt",
                                      defaultGenre.id,
                                      JsNull,
                                      TH_DESCRIPTION),
                    new PlatformGame("mahjonghilo",
                                      "MAHJONG HILO",
                                      "MJ",
                                      "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/MJ.png",
                                      "/game/mahjong",
                                      defaultGenre.id,
                                      Json.obj("battle_timer" -> 15),
                                      MJ_DESCRIPTION))
    // val PROTOCOL: String = serverAllowedProtocols(1)
    // val NODE_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(1)}"
    // val SCALA_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(0)}"
    // val COINICA_WEB_HOST: String = s"${PROTOCOL}://${serverAllowedURLs(2)}"
    // val MAILER_HOST: String = serverAllowedURLs(0)
    val hosts = List(new PlatformHost("scala_api", "api.coinica.net/s1"),
                    new PlatformHost("node_api", "api.coinica.net/s2"),
                    new PlatformHost("coinica", "coinica.net"),
                    new PlatformHost("mailer", "api.coinica.net/s1"))
    val currencies = List(new PlatformCurrency("usd-coin", "USDC"),
                          new PlatformCurrency("ethereum", "ETH"),
                          new PlatformCurrency("bitcoin", "BTC"))
    // insert only if table is empty..
    configRepo.add(new PlatformConfig(generateID, games, hosts, currencies))
  }
  // run all queries..
  vipBenefitQuery()
  genreQuery()
  systemDefaultConfigQuery()
  println("Database default generator definitions are written.")
}