// package akka

// import javax.inject.{ Inject, Named, Singleton }
// import java.util.UUID
// import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset, ZoneId }
// import scala.util.{ Success, Failure }
// import scala.concurrent.{ Await, Future }
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.duration._
// import scala.collection.mutable.{ ListBuffer, HashMap }
// import akka.util.Timeout
// import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
// import play.api.libs.ws.WSClient
// import models.domain._
// import models.domain.eosio.{ TableRowsRequest, GQCharacterGameHistory, GQBattleResult }
// import models.repo.{ OverAllGameHistoryRepo, DailyTaskRepo, TaskRepo, UserAccountRepo }
// import models.repo.eosio.{ GQCharacterDataRepo, GQCharacterGameHistoryRepo }
// import utils.lib.{ GhostQuestEOSIO, GQBattleCalculation }
// import models.domain.eosio.GQ.v2._
// import akka.common.objects._
// import utils.Config

// object GQSchedulerActorV2 {
//   val defaultTimeSet: Int            = Config.GQ_DEFAULT_BATTLE_TIMER
//   val scheduledTime : FiniteDuration = { defaultTimeSet }.minutes
//   var isIntialized  : Boolean        = false
//   val EOSTable: TableRowsRequest = new TableRowsRequest(
//                                         Config.GQ_CODE,
//                                         Config.GQ_TABLE,
//                                         Config.GQ_SCOPE,
//                                         None,
//                                         Some("uint64_t"),
//                                         None,
//                                         None,
//                                         None)
//   def props(characterRepo: GQCharacterDataRepo,
//             historyRepo: GQCharacterGameHistoryRepo,
//             gameTxHistory: OverAllGameHistoryRepo,
//             accountRepo: UserAccountRepo,
//             taskRepo: TaskRepo,
//             dailyTaskRepo: DailyTaskRepo,
//             ghostQuestEOSIO: GhostQuestEOSIO)(implicit system: ActorSystem) =
//     Props(classOf[GQSchedulerActorV2],
//           characterRepo,
//           historyRepo,
//           gameTxHistory,
//           accountRepo,
//           taskRepo,
//           dailyTaskRepo,
//           ghostQuestEOSIO,
//           system)
// }

// @Singleton
// class GQSchedulerActorV2 @Inject()(
//       characterRepo: GQCharacterDataRepo,
//       gQGameHistoryRepo: GQCharacterGameHistoryRepo,
//       gameTxHistory: OverAllGameHistoryRepo,
//       accountRepo: UserAccountRepo,
//       taskRepo: TaskRepo,
//       dailyTaskRepo: DailyTaskRepo,
//       ghostQuestEOSIO: GhostQuestEOSIO,
//       @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
//       @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
//     )(implicit system: ActorSystem ) extends Actor with ActorLogging {
//   implicit private val timeout: Timeout = new Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

//   override def preStart: Unit = {
//     super.preStart
//     // keep alive connection
//     akka.stream.scaladsl.Source.tick(0.seconds, 60.seconds, "GQSchedulerActorV2").runForeach(n => ())
//     system.actorSelection("/user/GQSchedulerActorV2").resolveOne().onComplete {
//       case Success(actor) =>
//         if (!GQSchedulerActorV2.isIntialized) {
//           GQSchedulerActorV2.isIntialized = true

//           // GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTimeSet)
//           // systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
//           log.info("GQ Scheduler Actor Initialized")
//         }
//       case Failure(ex) => // if actor is not yet created do nothing..
//     }
//   }

//   def receive: Receive = {
//     case REQUEST_BATTLE_NOW =>
//       try {
//         // get all characters on SC and store in GQBattleScheduler.characters
//         Await.ready(getEOSTableRows(Some("REQUEST_ON_BATTLE")), Duration.Inf)
//         Thread.sleep(1000)
//         // filter characters based on condition and battle all available characters
//         Await.ready(removeEliminatedAndWithdrawn(), Duration.Inf)
//         Thread.sleep(1000)
//         Await.ready(battleProcess(), Duration.Inf)
//         Thread.sleep(1000)
//         // broadcast characters no available enemy..
//         dynamicBroadcast ! ("BROADCAST_CHARACTER_NO_ENEMY", GQBattleScheduler.noEnemy.groupBy(_._2))
//         Thread.sleep(1000)
//         // cleanup memory by removing tracked data
//         GQBattleScheduler.characters.clear()
//         GQBattleScheduler.noEnemy.clear()

//         if (GQBattleScheduler.battleCounter.isEmpty) {
//           dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
//           GQBattleScheduler.battleCounter.clear()
//           defaultSchedule()
//         }
//         else {
//           val scBattleCounter = HashMap.empty[String, (UUID, GQBattleResult)]

//           GQBattleScheduler.battleCounter.map { counter =>
//             val winner = counter._2.characters.filter(_._2._2).head
//             val loser = counter._2.characters.filter(!_._2._2).head

//             for {
//               p1 <- accountRepo.getByID(winner._2._1)
//               p2 <- accountRepo.getByID(loser._2._1)
//               _ <- Await.ready({
//                 ghostQuestEOSIO.battleResult(counter._1.toString, (winner._1, p1.get.userGameID), (loser._1, p2.get.userGameID)).map {
//                   case Some(e) => scBattleCounter.addOne(e, counter)
//                   case e => null
//                 }
//               }, Duration.Inf)
//             } yield ()
//           }
//           Thread.sleep(5000)

//           if (!scBattleCounter.isEmpty) {
//             Await.ready(saveToGameHistory(scBattleCounter.toSeq), Duration.Inf)
//             Await.ready(insertOrUpdateSystemProcess(scBattleCounter.toSeq), Duration.Inf)
//             // wait prev txs finished and removed SC Battle Counter
//             Thread.sleep(1000)
//             scBattleCounter.clear()
//             // fetch SC table rows and save into GQBattleScheduler.eliminatedOrWithdrawn
//             Await.ready(getEOSTableRows(Some("REQUEST_REMOVE_NO_LIFE")), Duration.Inf)
//             Thread.sleep(1000)
//             // remove no life characters
//             // successfully removed characters in SC must be removed from DB
//             Await.ready(removedNoHpOnSc(), Duration.Inf)
//             Thread.sleep(1000)

//             Await.ready(for {
//               // check if theres existing chracters that has no life
//               hasNoLife <- characterRepo.getNoLifeCharacters
//               mergeSeq <- Future.successful(characterRepo.mergeSeq[GQCharacterData, Seq[GQCharacterData]](GQBattleScheduler.toRemovedCharacters.map(_._2).toSeq, hasNoLife))
//               _ <- Future.successful {
//                 if (!mergeSeq.isEmpty) {
//                   Await.ready(Future.successful(mergeSeq.map { data =>
//                                 for {
//                                   isRemoved <- characterRepo.remove(data.owner, data.key)
//                                   _ <- {
//                                     if (isRemoved > 0)
//                                       characterRepo.insertDataHistory(GQCharacterData.toCharacterDataHistory(data))
//                                     else
//                                       Future(0) // TODO: re-try if failed tx.
//                                   }
//                                 } yield (Thread.sleep(1000))
//                               }), Duration.Inf)
//                   Thread.sleep(2000)
//                   // broadcast users that DB has been update..
//                   // update again overall DB to make sure its updated..
//                   Await.ready(getEOSTableRows(Some("REQUEST_UPDATE_CHARACTERS_DB")), Duration.Inf)
//                   Thread.sleep(2000)

//                   Await.ready(Future.successful(GQBattleScheduler.isUpdatedCharacters
//                                   .map(_._2)
//                                   .toSeq
//                                   .map(characterRepo.updateOrInsertAsSeq(_))), Duration.Inf)
//                   Thread.sleep(1000)
//                   dynamicBroadcast ! "BROADCAST_DB_UPDATED"
//                 }
//               }
//             } yield (), Duration.Inf)
//             Thread.sleep(1000)

//             GQBattleScheduler.battleCounter.clear()
//             GQBattleScheduler.toRemovedCharacters.clear()
//             GQBattleScheduler.isUpdatedCharacters.clear()
//             GQBattleScheduler.eliminatedOrWithdrawn.clear()
//             defaultSchedule()
//           }
//           else {
//             dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
//             defaultSchedule()
//           }
//         }
//       } catch {
//         case _: Throwable => // reset all tracker into default..
//           GQBattleScheduler.noEnemy.clear()
//           GQBattleScheduler.toRemovedCharacters.clear()
//           GQBattleScheduler.characters.clear()
//           GQBattleScheduler.battleCounter.clear()
//           GQBattleScheduler.isUpdatedCharacters.clear()
//           GQBattleScheduler.eliminatedOrWithdrawn.clear()
//           defaultSchedule()
//       }

//     case _ => ()
//   }

//   private def removedNoHpOnSc(): Future[Any] = {
//     try {
//       // filter only characters that has no more life..
//       val eliminatedOrWithdrawn = GQBattleScheduler.eliminatedOrWithdrawn.filter(x => x._2.life < 1)
//       // remove from smartcontract
//       Future.sequence(eliminatedOrWithdrawn.map {
//         case (id, data) =>
//           for {
//             account <- accountRepo.getByID(data.owner)
//             result <- Future.successful {
//               if (account != None) {
//                 val acc: UserAccount =  account.get

//                 ghostQuestEOSIO.eliminate(acc.userGameID, id).map {
//                   case Some(txHash) => GQBattleScheduler.toRemovedCharacters.addOne(id, data)
//                   case None => ()
//                 }
//               }
//             }
//           } yield (result)
//       })
//     } catch { case _ : Throwable => Future(()) }
//   }
//   // challengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double)
//   private def insertOrUpdateSystemProcess(seq: Seq[(String, (UUID, GQBattleResult))]): Future[Seq[Unit]] = Future.successful {
//     seq.map { case (hash, (gameID, result)) =>
//       result.characters.map { v =>
//         dynamicProcessor ! DailyTask(v._2._1, Config.GQ_GAME_ID, 1)
//         dynamicProcessor ! ChallengeTracker(v._2._1, 1, (if(v._2._2) 2 else 0), 1, (if(v._2._2) 1 else 0))
//       }
//     }
//   }

//   private def saveToGameHistory(data: Seq[(String, (UUID, GQBattleResult))]): Future[Seq[Any]] = Future.sequence {
//     data.map { case (txHash, (gameID, result)) =>
//       val winner = result.characters.filter(_._2._2).head
//       val loser = result.characters.filter(!_._2._2).head
//       val time = Instant.now.getEpochSecond

//       Await.ready(
//         for {
//           winnerAcc <- accountRepo.getByID(winner._2._1)
//           loserAcc <- accountRepo.getByID(loser._2._1)
//         } yield ((new OverAllGameHistory(
//                               UUID.randomUUID,
//                               txHash,
//                               gameID.toString,
//                               Config.GQ_CODE,
//                               BooleanPredictions(winnerAcc.map(_.username).getOrElse(""), true, true, 1, 1, None),
//                               true,
//                               time),
//           new OverAllGameHistory(
//                               UUID.randomUUID,
//                               txHash,
//                               gameID.toString,
//                               Config.GQ_CODE,
//                               BooleanPredictions(loserAcc.map(_.username).getOrElse(""), true, false, 1, 0, None),
//                               true,
//                               time)),
//           new GQCharacterGameHistory(
//                         gameID.toString, // Game ID
//                         txHash,
//                         winner._2._1,
//                         winner._1,
//                         loser._2._1,
//                         loser._1,
//                         result.logs,
//                         time)), Duration.Inf)
//       .map {
//         case ((winner, loser), character) =>
//           // insert Tx and character contineously
//           // broadcast game result to connected users
//           // use live data to feed on history update..
//           Await.ready(for {
//             _ <- gQGameHistoryRepo.insertGameHistory(character)
//             _ <- gameTxHistory.add(winner)
//             _ <- gameTxHistory.add(loser)
//           } yield (), Duration.Inf)
//           // broadcast GQ game result
//           Thread.sleep(1000)
//           dynamicBroadcast ! Array(winner, loser)
//         case _ => ()
//       }
//     }
//   }
//   // make sure no eliminated or withdrawn characters on the list
//   // and shuflle remaining characters..
//   private def removeEliminatedAndWithdrawn(): Future[HashMap[String, GQCharacterData]] = {
//     for {
//       // remove no life characters and with max limit
//       removedNoLifeAndLimit <- Future.successful(GQBattleScheduler.characters.filter(x => !x._2.isNew || x._2.life >= 1))
//       _ <- Future.successful(GQBattleScheduler.characters.clear())
//       // make sure no eliminated or withdrawn characters on the list
//       removeEliminatedOrWithdrawn <- Future.successful(removedNoLifeAndLimit.filterNot(x => x._2.status > 1 || x._2.count >= x._2.limit))
//       // remove old tracked result, to changed with new one..
//       result <- Future.successful(GQBattleScheduler.characters.addAll(removeEliminatedOrWithdrawn))
//     } yield (result)
//   }

//   private def battleProcess(): Future[Unit] = Future.successful {
//     do {
//       val player: (String, GQCharacterData) = GQBattleScheduler.characters.head
//       if (GQBattleScheduler.characters.size == 1) {
//         GQBattleScheduler.characters.remove(player._1)
//         GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//       }
//       else {
//         // val availableCharacters: HashMap[String, GQCharacterData] = GQBattleScheduler.characters
//         val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
//         val filteredDateForBattle: Instant = now.plusDays(-7).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
//         // remove his other owned characters from the list
//         // val removedOwned: HashMap[String, GQCharacterData] = GQBattleScheduler.characters.filter(_._2.owner != player._2.owner)
//         // check chracters spicific history to avoid battling again as posible..
//         for {
//           ownedCharacters <- Await.ready({
//             gQGameHistoryRepo.getGameHistoryByUsernameCharacterIDAndDate(
//                                 player._2.owner,
//                                 player._1,
//                                 filteredDateForBattle.getEpochSecond,
//                                 now.toInstant(ZoneOffset.UTC).getEpochSecond)
//           }, Duration.Inf)
//           removedOwned <- Future.successful(GQBattleScheduler.characters.filter(_._2.owner != player._2.owner))
//           removed <- Await.ready(Future.successful {
//             removedOwned
//               .filterNot(ch => ownedCharacters.map(_.loserID).contains(ch._1))
//               .filterNot(ch => ownedCharacters.map(_.winnerID).contains(ch._1))
//           }, Duration.Inf)

//           _ <- Await.ready(Future.successful {
//             if (!removed.isEmpty && GQBattleScheduler.battleCounter.filter(_._2.characters.map(_._1).toSeq.contains(player._1)).isEmpty) {
//               // make sure battle of characters are not yet exists in GQBattleScheduler.battleCounter
//               val enemy: (String, GQCharacterData) = removed.head
//               if (GQBattleScheduler.battleCounter.filter(_._2.characters.map(_._1).toSeq.contains(enemy._1)).isEmpty) {
//                 val battle: GQBattleCalculation[GQCharacterData] = new GQBattleCalculation[GQCharacterData](player._2, enemy._2)
//                 // save result into battleCounter
//                 if (battle.result.equals(None) || battle.result.map(_.characters.size).getOrElse(0) < 2) {
//                   GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//                   GQBattleScheduler.noEnemy.addOne(enemy._1, enemy._2.owner)
//                 }
//                 else battle.result.map(x => GQBattleScheduler.battleCounter.addOne(x.id, x))
//               }
//               else {
//                 GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//                 GQBattleScheduler.noEnemy.addOne(enemy._1, enemy._2.owner)
//               }

//               GQBattleScheduler.characters.remove(player._1)
//               GQBattleScheduler.characters.remove(enemy._1)
//             }
//             else {
//               GQBattleScheduler.characters.remove(player._1)
//               GQBattleScheduler.noEnemy.addOne(player._1, player._2.owner)
//             }
//           }, Duration.Inf)
//         } yield ()
//       }
//     } while (!GQBattleScheduler.characters.isEmpty);
//   }

//   // recursive request no matter how many times till finished
//   private def getEOSTableRows(sender: Option[String]): Future[Unit] = Future.successful {
//     var hasNextKey: Option[String] = None
//     var hasRows: Seq[GQTable] = Seq.empty
//     do {
//       Await.ready(requestGhostQuestTableRow(new TableRowsRequest(Config.GQ_CODE,
//                                                       Config.GQ_TABLE,
//                                                       Config.GQ_SCOPE,
//                                                       None,
//                                                       Some("uint64_t"),
//                                                       None,
//                                                       None,
//                                                       hasNextKey), sender), Duration.Inf) map {
//         case Some(GQRowsResponse(rows, hasNext, nextKey, sender)) =>
//           hasNextKey = if (nextKey == "") None else Some(nextKey)
//           hasRows = rows
//         case _ =>
//           hasNextKey = None
//           hasRows = Seq.empty
//       }

//       Thread.sleep(2000)
//       if (!hasRows.isEmpty) Await.ready(processEOSTableResponse(sender, hasRows), Duration.Inf)
//     } while (hasNextKey != None);
//   }

//   private def processEOSTableResponse(sender: Option[String], rows: Seq[GQTable]): Future[Seq[Any]] = Future.sequence {
//     rows.map { row =>
//       // find account info and return ID..
//       accountRepo.getByName(row.username).map {
//         case Some(account) =>
//           // val username: String = row.username
//           val data: GQGame = row.data

//           val characters = data.characters.map { ch =>
//             val key = ch.key
//             val time = ch.value.createdAt
//             new GQCharacterData(key,
//                                 account.id,
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
//           }

//           sender match {
//             case Some("REQUEST_ON_BATTLE") =>
//               characters.map(v => GQBattleScheduler.characters.addOne(v.key, v))
//             case Some("REQUEST_REMOVE_NO_LIFE") =>
//               characters.map(v => GQBattleScheduler.eliminatedOrWithdrawn.addOne(v.key, v))
//             case Some("REQUEST_UPDATE_CHARACTERS_DB") =>
//               characters.map(v => GQBattleScheduler.isUpdatedCharacters.addOne(v.key, v))
//             case e => log.info("GQRowsResponse: unknown data")
//           }

//         case _ => Seq.empty
//       }
//     }
//   }

//   private def requestGhostQuestTableRow(req: TableRowsRequest, sender: Option[String]): Future[Option[GQRowsResponse]] =
//     ghostQuestEOSIO.getGhostQuestTableRows(req, sender)
//   private def systemBattleScheduler(timer: FiniteDuration): Unit = {
//     system.scheduler.scheduleOnce(timer) {
//       println("GQ BattleScheduler Starting")
//       GQBattleScheduler.nextBattle = 0
//       self ! REQUEST_BATTLE_NOW
//     }
//   }
//   private def defaultSchedule(): Unit = {
//     GQBattleScheduler.nextBattle = Instant.now().getEpochSecond + (60 * GQSchedulerActorV2.defaultTimeSet)
//     systemBattleScheduler(GQSchedulerActorV2.scheduledTime)
//   }
// }