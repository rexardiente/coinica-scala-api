package utils

class SchedulerModule extends com.google.inject.AbstractModule with play.api.libs.concurrent.AkkaGuiceSupport {
  override protected def configure() = {
    bindActor[akka.GQSchedulerActor]("GQSchedulerActor")
    bindActor[akka.SystemSchedulerActor]("SystemSchedulerActor")
  }
}
