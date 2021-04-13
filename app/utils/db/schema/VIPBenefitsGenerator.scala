package utils.db.schema

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import scala.concurrent.Future
import akka.actor.{Actor, ActorSystem, ActorLogging}
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.dao._
import models.domain.enum._
import models.domain.VIPBenefit

@Singleton
class VIPBenefitGenerator @Inject()(dao: VIPBenefitDAO,
                                    implicit val system: ActorSystem,
                                    val dbConfigProvider: DatabaseConfigProvider)
                                    extends HasDatabaseConfigProvider[utils.db.PostgresDriver]
                                    with Actor
                                    with ActorLogging {
  import profile.api._

  private def add(benefit: VIPBenefit): Future[Int] =
    db.run(dao.Query += benefit)

  override def preStart(): Unit = {
    super.preStart
    val benefits: Seq[VIPBenefit] = Seq(
      new VIPBenefit(VIP.BRONZE, 1.0, 10, 0.12, true, false, VIPBenefitAmount.BRONZE, VIPBenefitPoints.BRONZE, Instant.now()),
      new VIPBenefit(VIP.SILVER, 3.0, 20, 0.14, true, false, VIPBenefitAmount.SILVER, VIPBenefitPoints.SILVER, Instant.now()),
      new VIPBenefit(VIP.GOLD, 5.0, 30, 0.16, true, true, VIPBenefitAmount.GOLD, VIPBenefitPoints.GOLD, Instant.now()))
    // add into DB
    benefits.map(add)
    // after schema is generated..terminate akka actor gracefully
    context.stop(self)
  }

  override def postStop(): Unit = log.info("Schema VIP Benefits definitions are written")

  def receive = { _ => }  // do nothing..
}