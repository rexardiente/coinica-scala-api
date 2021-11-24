package utils.db.schema

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import scala.concurrent.Future
import akka.actor.{Actor, ActorSystem, ActorLogging}
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.repo._
import models.domain.enum._
import models.domain._
import utils.GameConfig._

@Singleton
class DBMockupGenerator @Inject()(
    gameRepo: GameRepo,
    genreRepo: GenreRepo,
    newsRepo: NewsRepo,
    challengeRepo: ChallengeRepo,
    overAllGameHistoryRepo: OverAllGameHistoryRepo,
    implicit val system: ActorSystem,
    val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[utils.db.PostgresDriver]
    with Actor
    with ActorLogging {
  import profile.api._
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private def generateID(): UUID = UUID.randomUUID()
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

  override def preStart(): Unit = {
    super.preStart

    // add into DB
    genreQuery()
    gamesQuery()
    // after schema is generated..terminate akka actor gracefully
    context.stop(self)
  }

  override def postStop(): Unit = log.info("Schema DBMockupGenerator definitions are written")
  def receive = { _ => }  // do nothing..
}