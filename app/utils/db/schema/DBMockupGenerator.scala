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

  private def gamesQuery(): Unit = {
    val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    val genreID: UUID = UUID.fromString("7633f64d-2caa-472b-9da7-e0feb439aefe")
    Seq(
        new Game(UUID.fromString("0f335579-1bf8-4f9e-8ede-eb204f5c0cba"), "GHOST QUEST", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/GQ.jpeg", "/game/ghostquest", genreID, Some(description)),
        new Game(UUID.fromString("1b977a2b-842e-430b-bd1b-c0bd3abe1c55"), "TREASURE HUNT", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/TH.jpeg", "/game/treasurehunt", genreID, Some(description)),
        new Game(UUID.fromString("74cd374c-6126-495a-a8a3-33db87caa511"), "MAHJONG HILO", "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/MJ.png", "/game/mahjong", genreID, Some(description))
    ).map(gameRepo.add(_))
  }
  private def genreQuery(): Unit = {
    genreRepo.add(new Genre(UUID.fromString("7633f64d-2caa-472b-9da7-e0feb439aefe"), "LUCKY", None))
  }
  private def newsQuery(): Unit = {
    // val title: String = "Lorem Ipsum"
    // val subTitle: String = "What is Lorem Ipsum?"
    // val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    // // temporary list of news and
    // // add list to news tbl..
    // newsRepo ++= Seq(
    //   News(s"${title} 1", s"${subTitle} 1", description, "author 1", "sample url"),
    //   News(s"${title} 2", s"${subTitle} 2", description, "author 2", "sample url"),
    //   News(s"${title} 3", s"${subTitle} 3", description, "author 3", "sample url"))
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
    // challenge()
    // gameHistory()
    // after schema is generated..terminate akka actor gracefully
    context.stop(self)
  }

  override def postStop(): Unit = log.info("Schema DBMockupGenerator definitions are written")
  def receive = { _ => }  // do nothing..
}