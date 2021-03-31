package utils

class SchedulerModule extends com.google.inject.AbstractModule with play.api.libs.concurrent.AkkaGuiceSupport {
  override protected def configure() = {
    bindActor[akka.DynamicBroadcastActor]("DynamicBroadcastActor")
    bindActor[akka.DynamicSystemProcessActor]("DynamicSystemProcessActor")
    bindActor[akka.GQSchedulerActorV2]("GQSchedulerActorV2")
    bindActor[akka.SystemSchedulerActor]("SystemSchedulerActor")
  }
}
