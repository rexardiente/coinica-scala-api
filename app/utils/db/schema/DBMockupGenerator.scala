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
    newsRepo: NewsRepo,
    challengeRepo: ChallengeRepo,
    implicit val system: ActorSystem,
    val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[utils.db.PostgresDriver]
    with Actor
    with ActorLogging {
  import profile.api._

  private def newsQuery(): Unit = {
    val title: String = "Lorem Ipsum"
    val subTitle: String = "What is Lorem Ipsum?"
    val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    // temporary list of news and
    // add list to news tbl..
    newsRepo ++= Seq(
      News(s"${title} 1", s"${subTitle} 1", description, "author 1", List("images 1")),
      News(s"${title} 2", s"${subTitle} 2", description, "author 2", List("images 2")),
      News(s"${title} 3", s"${subTitle} 3", description, "author 3", List("images 3")))
  }

  private def challenge(): Unit = {
    val name: String = "GQ"
    val description: String = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
    val timeNow: Instant = Instant.now()

    challengeRepo ++= Seq(Challenge(name,
                                description,
                                timeNow,
                                Instant.ofEpochSecond(timeNow.getEpochSecond + 86400),
                                true))
  }

  override def preStart(): Unit = {
    super.preStart

    // add into DB
    newsQuery()
    challenge()
    // after schema is generated..terminate akka actor gracefully
    context.stop(self)
  }

  override def postStop(): Unit = log.info("Schema DBMockupGenerator definitions are written")
  def receive = { _ => }  // do nothing..
}