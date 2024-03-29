package utils

class SchedulerModule extends com.google.inject.AbstractModule with play.api.libs.concurrent.AkkaGuiceSupport {
  override protected def configure() = {
    bindActor[akka.DynamicBroadcastActor]("DynamicBroadcastActor")
    bindActor[akka.DynamicSystemProcessActor]("DynamicSystemProcessActor")
    bindActor[akka.GhostQuestSchedulerActor]("GhostQuestSchedulerActor")
    bindActor[akka.SystemSchedulerActor]("SystemSchedulerActor")
  }
}
