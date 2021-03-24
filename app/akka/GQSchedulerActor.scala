// package akka

// import javax.inject.{ Inject, Singleton }
// import java.util.UUID
// import java.time.Instant
// import scala.util.{ Success, Failure }
// import scala.concurrent.Future
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.duration._
// import scala.collection.mutable.{ ListBuffer, HashMap }
// import com.typesafe.config.{ Config, ConfigFactory }
// import akka.util.Timeout
// import akka.actor.{
//         ActorRef,
//         Actor,
//         ActorSystem,
//         Props,
//         ActorLogging,
//         Cancellable }
// import play.api.libs.ws.WSClient
// import akka.common.objects.{
//         GQSchedulerStatus,
//         GQBattleScheduler,
//         OnUpdateGQList,
//         VerifyGQUserTable,
//         RemoveCharacterWithNoLife }
// import models.domain.OutEvent
// import models.domain.eosio.{ TableRowsRequest, GQCharacterGameHistory, GQBattleResult }
// import models.domain.{ OverAllGameHistory, GameType, GQGameHistory }
// import models.repo.OverAllGameHistoryRepo
// import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
// import models.service.GQSmartContractAPI
// import utils.lib.{ EOSIOSupport, GQBattleCalculation }
// import models.domain.eosio.GQ.v2._

// object GQSchedulerActor {
//   var isIntialized: Boolean = false
//   val defaultTime: Int = ConfigFactory.load().getInt("platform.games.GQ.battle.timer")
//   val defaultTimer: FiniteDuration = { defaultTime }.minutes
//   val eosTblRowsRequest: TableRowsRequest = new TableRowsRequest(
//                                                   "ghostquest",
//                                                   "users",
//                                                   "ghostquest",
//                                                   None,
//                                                   Some("uint64_t"),
//                                                   None,
//                                                   None,
//                                                   None)
//   def props(characterRepo: GQCharacterDataRepo,
//             historyRepo: GQCharacterGameHistoryRepo,
//             gameTxHistory: OverAllGameHistoryRepo,
//             eosio: EOSIOSupport,
//             smartcontract: GQSmartContractAPI)(implicit system: ActorSystem) =
//     Props(classOf[GQSchedulerActor], characterRepo, historyRepo, gameTxHistory, eosio, smartcontract, system)
// }

// @Singleton
// class GQSchedulerActor @Inject()(
//       characterRepo: GQCharacterDataRepo,
//       gQGameHistoryRepo: GQCharacterGameHistoryRepo,
//       gameTxHistory: OverAllGameHistoryRepo, // overall history for games..
//       support: EOSIOSupport,
//       eosio: GQSmartContractAPI)(implicit system: ActorSystem) extends Actor with ActorLogging {
//   implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)
//   private val dynamicBroadcastActor: ActorRef = system.actorOf(Props(classOf[DynamicBroadcastActor], None, system))

//   override def preStart: Unit = {
//     super.preStart
//     // check if intializer is the GQSchedulerActor module..
//     system.actorSelection("/user/GQSchedulerActor").resolveOne().onComplete {
//       case Success(actor) =>
//         if (!GQSchedulerActor.isIntialized) {
//           // scheduled 5minutes to start battle..
//           // systemBattleScheduler(GQSchedulerActor.defaultTimer)
//           // set true if actor already initialized
//           // GQSchedulerActor.isIntialized = true
//           log.info("Ghost Quest Scheduler Actor Initialized")
//         }
//       case Failure(ex) => // if actor is not yet created do nothing..
//     }
//   }

//   def receive: Receive = {
//     case GQSchedulerStatus =>
//       GQBattleScheduler.battleStatus match {
//         case "on_update" =>
//           GQBattleScheduler.nextBattle = 0 // reset timer to zero
//           self ! OnUpdateGQList("onupdate")

//         case "to_battle" =>
//           self ! OnUpdateGQList("onbattle")

//         case "GQ_battle_finished" => {
//           support.unlockWalletAPI()
//           // insert batch mode
//           // Insert first on smartcontract, track where the insertion failed
//           // failed tx will be removed to list and will not be added to DB
//           GQBattleScheduler.battleCounter.map { count =>
//             val winner = count._2.characters.filter(_._2._2).head
//             val loser = count._2.characters.filter(!_._2._2).head
//             val request: Seq[(String, String)] = Seq((winner._1, winner._2._1), (loser._1, loser._2._1))

//             eosio.battleAction(count._1, request).map {
//               case Some(e) =>
//               case e =>
//                 GQBattleScheduler.smartcontractTxFailed += count._1
//                 // track failed tx here..
//                 log.error("Error: Battle ")
//             }
//             Thread.sleep(300)
//           }
//           // remove failed txs on the list before inserting to DB
//           GQBattleScheduler.battleCounter.filterNot(ch => GQBattleScheduler.smartcontractTxFailed.contains(ch._1))
//           Thread.sleep(2000)
//           // convert into OverAllGameHistory
//           saveHistoryDB(GQBattleScheduler.battleCounter)
//         }

//         case "GQ_insert_DB" =>
//           Thread.sleep(1000)
//           support.lockAllWallets()
//           // update latest characters on DB..
//           self ! VerifyGQUserTable(GQSchedulerActor.eosTblRowsRequest, Some("update_characters"))
//           // on battle start reset timer to 0 and set new timer until the battle finished
//           // set back the time for next battle..
//           GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * 5)
//           // broadcast to all connected users the next GQ battle
//           dynamicBroadcastActor ! "BROADCAST_NEXT_BATTLE"
//           systemBattleScheduler(GQSchedulerActor.defaultTimer)
//         case _ => // do nothing
//       }

//     case OnUpdateGQList(req) => req match {
//       case "onupdate" =>
//         self ! VerifyGQUserTable(GQSchedulerActor.eosTblRowsRequest, Some(req))

//       case "RemoveCharacterWithNoLife" =>
//         self ! VerifyGQUserTable(GQSchedulerActor.eosTblRowsRequest, Some(req))

//       case "onbattle" =>
//         val characters: HashMap[String, GQCharacterData] = GQBattleScheduler.characters
//         // remove characters that are newly created..
//         // val removedNew = characters.filterNot(_._2.isNew)
//         val filtered = characters.filterNot(x => x._2.isNew == true || x._2.life <= 0 || x._2.count >= x._2.limit)
//         // remove characters that has no life
//         // val removedNoLife = removedNew.filterNot(_._2.life <= 0)
//         // remove characters that exceeds battle limit
//         // val removedExceedBattleLimit = removedNoLife.filter(x => x._2.count < x._2.limit)
//         // make sure no eliminated or withdrawn characters on the list
//         val isEliminatedOrWithdrawn = filtered.filterNot(x => x._2.status == 2  || x._2.status == 3)
//         // shuffle all list of characters to play
//         val availableCharacters = scala.util.Random.shuffle(isEliminatedOrWithdrawn)
//         // start the battle..
//         battleProcess(availableCharacters)
//         Thread.sleep(10000)
//         // all finished battle will be recorded into `battleCounter`
//         // save all characters that has no available to play
//         GQBattleScheduler.characters.clear
//         GQBattleScheduler.battleStatus = "GQ_battle_finished"
//         self ! GQSchedulerStatus

//       case "Unknown" => log.info("OnUpdateGQList Unknown")
//     }

//     case GQRowsResponse(rows, hasNext, nextKey, sender) => {
//       rows.foreach { row =>
//         val username: String = row.username
//         val data: GQGame = row.data

//         data.characters.foreach { ch =>
//           val key = ch.key
//           val time = ch.value.createdAt
//           val chracterInfo = new GQCharacterData(
//                                 key,
//                                 username,
//                                 ch.value.life,
//                                 ch.value.hp,
//                                 ch.value.`class`,
//                                 ch.value.level,
//                                 ch.value.status,
//                                 ch.value.attack,
//                                 ch.value.defense,
//                                 ch.value.speed,
//                                 ch.value.luck,
//                                 ch.value.limit,
//                                 ch.value.count,
//                                 if (time <= Instant.now().getEpochSecond - (60 * 5)) false else true,
//                                 time)
//           sender match {
//             case Some("onupdate") => GQBattleScheduler.characters.addOne(key, chracterInfo)
//             case Some("RemoveCharacterWithNoLife") => GQBattleScheduler.eliminatedOrWithdrawn.addOne(key, chracterInfo)
//             case Some("update_characters") => GQBattleScheduler.isUpdatedCharacters.addOne(key, chracterInfo)
//             case _ => log.info("GQRowsResponse: unknown data")
//           }
//         }
//       }

//        if (hasNext) self ! VerifyGQUserTable(new TableRowsRequest("ghostquest",
//                                                                   "users",
//                                                                   "ghostquest",
//                                                                   None,
//                                                                   Some("uint64_t"),
//                                                                   None,
//                                                                   None,
//                                                                   Some(nextKey)), sender)
//        if (!hasNext) {
//           sender match {
//             case Some("onupdate") =>
//               GQBattleScheduler.battleStatus = "to_battle"
//               self ! GQSchedulerStatus

//             case Some("RemoveCharacterWithNoLife") =>
//               GQBattleScheduler.isRemoving = false
//               self ! RemoveCharacterWithNoLife

//             case Some("update_characters") =>
//               // save to another variable and clear the dynamic result
//               val toSeq: Seq[GQCharacterData] = GQBattleScheduler.isUpdatedCharacters.map(_._2).toSeq
//               GQBattleScheduler.isUpdatedCharacters.clear
//               characterRepo.updateOrInsertAsSeq(toSeq)

//             case _ =>
//           }
//        }
//     }

//     case RemoveCharacterWithNoLife =>
//       // set to remove status while fetching..
//       if (GQBattleScheduler.isRemoving)
//         self ! OnUpdateGQList("RemoveCharacterWithNoLife")
//       else {
//         // to make sure that validations is finished
//         // Thread.sleep(5000)
//         // filter characters with status eliminated or withdrawn
//         val eliminatedOrWithdrawn: HashMap[String, GQCharacterData] = GQBattleScheduler.eliminatedOrWithdrawn
//         val filteredByStatus = eliminatedOrWithdrawn.filter(x => x._2.life <= 0)
//         // remove from smartcontract
//         filteredByStatus.map { case (id, data) =>
//           eosio.removeCharacter(data.owner, id).map {
//             // Remove from the Character Data DB
//             case Some(x) =>
//               characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
//             // TODO: check the reason why it is failed..
//             case None => log.info("Error: removing character from smartcontract")
//           }
//         }
//         // clean back the list to get ready for later battle schedule..
//         GQBattleScheduler.eliminatedOrWithdrawn.clear
//         GQBattleScheduler.isRemoving = false
//         GQBattleScheduler.battleStatus = "GQ_insert_DB"
//         self ! GQSchedulerStatus
//       }


//     case VerifyGQUserTable(request, sender) =>
//       eosio.getGQUsers(request, sender).map { response =>
//         // check if battle scehduler requested..
//         if (sender == Some("onupdate"))
//           response.map(self ! _).getOrElse({
//             // broadcast to all connected users the next GQ battle
//             GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * 5)
//             dynamicBroadcastActor ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
//             systemBattleScheduler(GQSchedulerActor.defaultTimer)
//           })
//         else response.map(self ! _)
//       }

//     case e => log.info("Error: Unkown data received")
//   }

//   def battleProcess(params: HashMap[String, GQCharacterData]): Unit = {
//     val characters: HashMap[String, GQCharacterData] = params
//     do {
//       val player: (String, GQCharacterData) = characters.head
//       if (characters.size == 1) {
//         characters.remove(player._1)
//         GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//       }
//       else {
//         // remove his other owned characters from the list (remove 3 and 4 remaining)
//         val removedOwned: HashMap[String, GQCharacterData] =
//           characters.filterNot(_._2.owner == player._2.owner)
//         // check chracters spicific history to avoid battling again as posible..
//         gQGameHistoryRepo.getByUsernameAndCharacterID(player._1, player._2.owner).map { history =>
//           removedOwned
//             .filterNot(ch => history.map(_.loserID).contains(ch._1))
//             .filterNot(ch => history.map(_.winnerID).contains(ch._1))
//         }
//         // add delay to avoid conflict when process takes too long and repeating battles each characters..
//         Thread.sleep(2000)
//         if (!removedOwned.isEmpty) {
//           val enemy: (String, GQCharacterData) = removedOwned.head
//           val battle: GQBattleCalculation[GQCharacterData] = new GQBattleCalculation[GQCharacterData](player._2, enemy._2)

//           if (!battle.result.equals(None))
//             // save result into battleCounter
//             battle.result.map(x => GQBattleScheduler.battleCounter.addOne(x.id, x))
//           else {
//             GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//             GQBattleScheduler.noEnemy.addOne(enemy._1, enemy._2.owner)
//           }
//           characters.remove(player._1)
//           characters.remove(enemy._1)
//         }
//         else {
//           characters.remove(player._1)
//           GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//         }
//       }
//     } while (!characters.isEmpty);
//   }

//   def saveHistoryDB(data: HashMap[UUID, GQBattleResult]): Unit = {
//     data.map { count =>
//       val winner = count._2.characters.filter(_._2._2).head
//       val loser = count._2.characters.filter(!_._2._2).head
//       val time = Instant.now
//       ((new OverAllGameHistory(
//                             UUID.randomUUID,
//                             count._1,
//                             "ghostquest",
//                             GQGameHistory(winner._1, "WIN", true),
//                             true,
//                             time),
//         new OverAllGameHistory(
//                             UUID.randomUUID,
//                             count._1,
//                             "ghostquest",
//                             GQGameHistory(loser._1, "WIN", false),
//                             true,
//                             time)),
//       new GQCharacterGameHistory(
//                       count._1.toString, // Game ID
//                       winner._2._1,
//                       winner._1,
//                       loser._2._1,
//                       loser._1,
//                       count._2.logs,
//                       time.getEpochSecond))
//     }.map { case ((winner, loser), character) =>
//       // insert Tx and character contineously
//       gQGameHistoryRepo.insert(character)
//       // broadcast game result to connected users
//       // use live data to feed on history update..
//       gameTxHistory
//         .add(winner)
//         .map(x => if (x > 0) dynamicBroadcastActor ! winner else log.info("Error: Game Tx Insertion"))
//       gameTxHistory
//         .add(loser)
//         .map(x => if (x > 0) dynamicBroadcastActor ! loser else log.info("Error: Game Tx Insertion"))
//     }

//     Thread.sleep(5000)
//     // broadcast to spicific user if his characters doesnt have enemy..
//     dynamicBroadcastActor ! ("BROADCAST_CHARACTER_NO_ENEMY", GQBattleScheduler.noEnemy.groupBy(_._2))

//     GQBattleScheduler.battleCounter.clear
//     GQBattleScheduler.noEnemy.clear
//     // remove eliminated characters on the smartcontract
//     GQBattleScheduler.isRemoving = true
//     self ! RemoveCharacterWithNoLife
//   }

//   def defaultThreadSleep(): Unit = Thread.sleep(1000)

//   def systemBattleScheduler(timer: FiniteDuration): Cancellable = {
//     system.scheduler.scheduleOnce(timer) {
//       GQBattleScheduler.battleStatus = "on_update"
//       self ! GQSchedulerStatus
//     }
//   }
// }