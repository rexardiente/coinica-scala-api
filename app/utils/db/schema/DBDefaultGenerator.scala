package utils.db.schema

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.json._
import akka.actor.{ Actor, ActorSystem, ActorLogging }
import models.dao.VIPBenefitDAO
import models.repo.{ GameRepo, GenreRepo, PlatformConfigRepo }
import models.service.PlatformConfigService
import models.domain.enum._
import models.domain._
import utils.SystemConfig._

@Singleton
class DBDefaultGenerator @Inject()(
    dao: VIPBenefitDAO,
    gameRepo: GameRepo,
    genreRepo: GenreRepo,
    configRepo: PlatformConfigRepo,
    val dbConfigProvider: DatabaseConfigProvider)
    (implicit system: ActorSystem)
    extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with Actor with ActorLogging {
  import profile.api._
  // default variables
  private val defaultGenre: Genre = new Genre(generateID, "LUCKY", Some("Play with your luck."))
  // queries here...
  private def generateID(): UUID = UUID.randomUUID()
  private def vipBenefitQuery(): Future[Seq[Int]] = {
    val querySet = Seq(new VIPBenefit(VIP.BRONZE, 1.0, .10, 0.12, false, false, VIPBenefitAmount.BRONZE, VIPBenefitPoints.BRONZE, instantNowUTC()),
                      new VIPBenefit(VIP.SILVER, 3.0, .20, 0.14, false, false, VIPBenefitAmount.SILVER, VIPBenefitPoints.SILVER, instantNowUTC()),
                      new VIPBenefit(VIP.GOLD, 5.0, .30, 0.16, false, true, VIPBenefitAmount.GOLD, VIPBenefitPoints.GOLD, instantNowUTC()))

    Future.sequence(querySet.map(x => db.run(dao.Query += x)))
  }
  // if genre table is not empty do nothing..
  private def genreQuery(): Future[Int] = {
    for {
      genres <- genreRepo.all()
      update <- if (genres.size > 0) Future.successful(0) else genreRepo.add(defaultGenre)
    } yield (update)
  }
  private def systemDefaultConfigQuery(): Future[Int] = {
    // insert only if table is empty..
    configRepo.add(new PlatformConfig(generateID, initialGames, initialHosts, initialCurrencies))
  }
  override def preStart(): Unit = {
    super.preStart
    // run all queries..
    vipBenefitQuery()
    genreQuery()
    systemDefaultConfigQuery()
    Thread.sleep(3000)
    // after variables are updated..terminate akka actor gracefully
    context.stop(self)
  }
  override def postStop(): Unit = log.info("Database default generator definitions are written")
  def receive = { _ => }  // do nothing..
}