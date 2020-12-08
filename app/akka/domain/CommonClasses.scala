package akka.domain.common.objects

import akka.actor.ActorRef

case class Connect(connection: ActorRef) extends AnyVal
case class Disconnect(disconnection: ActorRef) extends AnyVal
object BattleScheduler
object LoadGQUserTable