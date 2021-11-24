package utils.db.schema

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
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
    val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private def generateID(): UUID = UUID.randomUUID()
  private def vipBenefitQuery(): Future[Seq[Int]] = {
    val querySet = Seq(new VIPBenefit(VIP.BRONZE, 1.0, .10, 0.12, false, false, VIPBenefitAmount.BRONZE, VIPBenefitPoints.BRONZE, Instant.now()),
                      new VIPBenefit(VIP.SILVER, 3.0, .20, 0.14, false, false, VIPBenefitAmount.SILVER, VIPBenefitPoints.SILVER, Instant.now()),
                      new VIPBenefit(VIP.GOLD, 5.0, .30, 0.16, false, true, VIPBenefitAmount.GOLD, VIPBenefitPoints.GOLD, Instant.now()))

    Future.sequence(querySet.map(x => db.run(dao.Query += x)))
  }
  private def genreQuery(): Future[Int] = genreRepo.add(new Genre(generateID, "LUCKY", None))
  private def gamesQuery(): Future[Seq[Int]] = {
    val GQ_DESCRIPTION: String = """Idle game with character-battle. You need only to draw loot box and summon ghosts and send them in battle field. If your ghost wins, you can get rewrard."""
    val TH_DESCRIPTION: String = """Seeking for treasure in three different ocean maps without being spotted by rival pirates!"""
    val MJ_DESCRIPTION: String = """High-low game with mahjong tiles, if you can complete a hand with 14 tiles until 33rd turn, you get a bonus according to your hand score ranking."""

    Future.sequence(Seq(
      new Game(GQ_GAME_ID,
              "GHOST QUEST",
              "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/GQ.jpeg",
              "/game/ghostquest",
              generateID,
              Some(GQ_DESCRIPTION)),
      new Game(TH_GAME_ID,
              "TREASURE HUNT",
              "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/TH.jpeg",
              "/game/treasurehunt",
              generateID,
              Some(TH_DESCRIPTION)),
      new Game(MJHilo_GAME_ID,
              "MAHJONG HILO",
              "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/MJ.png",
              "/game/mahjong",
              generateID,
              Some(MJ_DESCRIPTION))
    ).map(gameRepo.add(_)))
  }

  // run all queries..
  for {
    _ <- vipBenefitQuery()
    _ <- genreQuery()
    _ <- gamesQuery()
  } yield ()
  println("Database default generator definitions are written")
}