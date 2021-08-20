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
import utils.Config._

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

  private val genreID: UUID = UUID.randomUUID
  private def genreQuery(): Unit = genreRepo.add(new Genre(genreID, "LUCKY", None))
  private def gamesQuery(): Unit = {
    val GQ_DESCRIPTION: String = """Idle game with character-battle. You need only to draw loot box and summon ghosts and send them in battle field. If your ghost wins, you can get rewrard."""
    val TH_DESCRIPTION: String = """Seeking for treasure in three different ocean maps without being spotted by rival pirates!"""
    val MJ_DESCRIPTION: String = """High-low game with mahjong tiles, if you can complete a hand with 14 tiles until 33rd turn, you get a bonus according to your hand score ranking."""

    Seq(
        new Game(GQ_GAME_ID, "GHOST QUEST", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/GQ.jpeg", "/game/ghostquest", genreID, Some(GQ_DESCRIPTION)),
        new Game(TH_GAME_ID, "TREASURE HUNT", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/TH.jpeg", "/game/treasurehunt", genreID, Some(TH_DESCRIPTION)),
        new Game(MJHilo_GAME_ID, "MAHJONG HILO", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/MJ.png", "/game/mahjong", genreID, Some(MJ_DESCRIPTION))
    ).map(gameRepo.add(_))
  }

  private def newsQuery(): Unit = {
    // temporary list of news and
    // add list to news tbl..
    newsRepo ++= Seq(
      News("Beta Launch", "", "Launching your product in beta, knowingly releasing an unfinished product with known bugs and unknown bugs to users, is a great way to identify and rid its most critical bugs, usability friction, and bad performance before releasing it in its finished version", "Coinica", ""),
      News("Treasure Hunt", "Game", "Open tiles without pirates and test your luck.", "Coinica", ""),
      News("Ghost Quest", "Game", "Summon characters and earn points on ever battle win.", "Coinica", ""),
      News("Wallet Support", "BTC, ETH and USDC", "Cryptocurrency is a form of payment that can be exchanged online for goods and services. Many companies have issued their own currencies, often called tokens, and these can be traded specifically for the good or service that the company provides. Think of them as you would arcade tokens or casino chips.", "Coinica", ""),
      News("Mahjong Hilo", "Game", "Authentic Japanese mahjong with riichi mechanics and dora tiles.", "Coinica", ""))
  }
  private def challenge(): Unit = {
    // val name: String = "GQ"
    // val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    // val timeNow: Instant = Instant.now()

    // challengeRepo.getSize().map { x =>
    //   if (x.equals(0)) {
    //     challengeRepo ++= Seq(
    //       Challenge(name,
    //                 description,
    //                 timeNow,
    //                 Instant.ofEpochSecond(timeNow.getEpochSecond + 86400),
    //                 true))
    //   }
    // }
  }
  private def gameHistory(): Unit = {
    // overAllGameHistoryRepo.getSize().map { x =>
    //   if (x.equals(0)) {
    //     overAllGameHistoryRepo ++= Seq(
    //       OverAllGameHistory(UUID.randomUUID,
    //                             UUID.randomUUID,
    //                             "Ghost Quest",
    //                             "https://i.imgur.com/r77fFKE.jpg", // ICON URL to use..
    //                             List(GameType("user1", true, 3.00), GameType("user2", false, 3.00)),
    //                             false, // update `confirmed` when system get notified from EOSIO net
    //                             Instant.now()),
    //       OverAllGameHistory(UUID.randomUUID,
    //                             UUID.randomUUID,
    //                             "Ghost Quest",
    //                             "https://i.imgur.com/vPGoLvP.jpg", // ICON URL to use..
    //                             List(PaymentType("user2", "receive", 3.00)),
    //                             true, // update `confirmed` when system get notified from EOSIO net
    //                             Instant.now()))
    //   }
    // }

    // val name: String = "GQ"
    // val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    // val timeNow: Instant = Instant.now()

    // challengeRepo ++= Seq(Challenge(name,
    //                             description,
    //                             timeNow,
    //                             Instant.ofEpochSecond(timeNow.getEpochSecond + 86400),
    //                             true))
  }

  override def preStart(): Unit = {
    super.preStart

    // add into DB
    genreQuery()
    Thread.sleep(1000)
    gamesQuery()
    newsQuery()
    // challenge()
    // gameHistory()
    // after schema is generated..terminate akka actor gracefully
    context.stop(self)
  }

  override def postStop(): Unit = log.info("Schema DBMockupGenerator definitions are written")
  def receive = { _ => }  // do nothing..
}