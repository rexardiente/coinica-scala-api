package utils.db.schema

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ Actor, ActorSystem, ActorLogging }
import models.domain.PlatformConfig
import models.service.PlatformConfigService
import utils.SystemConfig._

@Singleton
class LoadSystemConfig @Inject()(configService: PlatformConfigService)(implicit system: ActorSystem) extends Actor with ActorLogging {
  // load default static configs
  private def loadDefaultHostsConfigs(): Future[Option[Unit]] = {
    configService
      .loadConfig()
      .map(_.map { conf =>
        // set default URI's
        conf.hosts.foreach { x =>
          x.name match {
            case "node_api" => NODE_SERVER_URI = x.getURL()
            case "scala_api" => SCALA_SERVER_URI = x.getURL()
            case "mailer" => MAILER_HOST = x.getURL()
            case "coinica" => COINICA_WEB_HOST = x.getURL()
          }
        }
      })
  }
  // load default static configs
  private def loadDefaultStaticConfigs(): Future[Option[Unit]] = {
    configService
      .loadConfig()
      .map(_.map { conf =>
        // set default timers
        DEFAULT_SYSTEM_SCHEDULER_TIMER = conf.tokenExpiration
        DEFAULT_EXPIRATION = conf.defaultscheduler
        DEFAULT_WEI_VALUE = BigDecimal(conf.wei)
        // set others..
        SUPPORTED_CURRENCIES = conf.currencies
      })
  }
  override def preStart(): Unit = {
    super.preStart
    for {
      _ <- loadDefaultHostsConfigs()
      _ <- loadDefaultStaticConfigs()
      // after variables are updated..terminate akka actor gracefully
      _ <- Future.successful { context.stop(self) }
    } yield ()
  }
  override def postStop(): Unit = log.info("System default configurations are written")
  def receive = { _ => }  // do nothing..
}
