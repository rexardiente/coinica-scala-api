package akka

import javax.inject.{ Inject, Named, Singleton }
import java.util.{ concurrent, UUID }
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset, ZoneId }
import scala.util.{ Success, Failure }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.util.Timeout
import akka.actor.{ ActorRef, Actor, ActorSystem, Props, ActorLogging, Cancellable }
import models.domain._
import models.domain.eosio._
import models.service._
import utils.lib._
import utils.Config._

object GhostQuestSchedulerActor {
  val defaultTimeSet: Int            = GQ_DEFAULT_BATTLE_TIMER
  val scheduledTime : FiniteDuration = { defaultTimeSet }.minutes
  var isIntialized  : Boolean        = false
  var nextBattle    : Long           = 0
  def props(historyService: HistoryService,
            userAccountService: UserAccountService,
            ghostQuestCharacterService: GhostQuestCharacterService,
            ghostQuestGameService: GhostQuestGameService)(implicit system: ActorSystem) =
    Props(classOf[GhostQuestSchedulerActor],
          historyService,
          userAccountService,
          ghostQuestCharacterService,
          ghostQuestGameService,
          system)
}

@Singleton
class GhostQuestSchedulerActor @Inject()(
      historyService: HistoryService,
      userAccountService: UserAccountService,
      ghostQuestCharacterService: GhostQuestCharacterService,
      ghostQuestGameService: GhostQuestGameService,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
    )(implicit system: ActorSystem ) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, concurrent.TimeUnit.SECONDS)

  override def preStart: Unit = {
    super.preStart
    // keep alive connection
    // akka.stream.scaladsl.Source.tick(0.seconds, 60.seconds, "GhostQuestSchedulerActor").runForeach(n => ())
    system.actorSelection("/user/GhostQuestSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!GhostQuestSchedulerActor.isIntialized) {
          GhostQuestSchedulerActor.isIntialized = true

          // GhostQuestSchedulerActor.nextBattle = Instant.now().getEpochSecond + (60 * GhostQuestSchedulerActor.defaultTimeSet)
          // systemBattleScheduler(GhostQuestSchedulerActor.scheduledTime)
          log.info("GQ Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
  	case "REQUEST_BATTLE_NOW" =>
      for {
        _ <- ghostQuestCharacterService.removeAllGhostQuestCharacter
        // get all characters on smartcontract, ready to save into DB..
        currentTable <- ghostQuestGameService.getAllCharacters
        // get characters from table, flatten Seq[Seq[]] to Seq[]
        extractCharacters <- Future.successful(currentTable.map(_.map(_.game_data.characters).flatten))
        // save to DB
        _ <- Future.successful(extractCharacters.map(ghostQuestCharacterService.insertGhostQuestCharacters(_)))
        // get save characters from DB
        availableCharacters <- ghostQuestCharacterService.allGhostQuestCharacter()
        // convert availableCharacters into HashMap[String, GhostQuestCharacter]
        hashMapCharacters <- Future.successful(HashMap(availableCharacters.map(v => (v.key, v)): _*))
        // process battle
        (noEnemies, listOfBattle) <- battleProcess(hashMapCharacters)
        // broadcast characters no available enemy..
        _ <- Future.successful(dynamicBroadcast ! ("BROADCAST_CHARACTER_NO_ENEMY", noEnemies.groupBy(_._2)))
        _ <- Future.successful {
          if (listOfBattle.isEmpty) {
            dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
            defaultSchedule()
          }
          else {
            val scUpdatedBattles = HashMap.empty[String, (UUID, GhostQuestBattleResult)]
            // TODO: TO be continued...
            listOfBattle.map { counter =>
              val winner = counter._2.characters.filter(_._2._2).head
              val loser = counter._2.characters.filter(!_._2._2).head

              ghostQuestGameService.battleResult(counter._1.toString, (winner._1, winner._2._1), (loser._1, loser._2._1)).map {
                  case Some(e) => scUpdatedBattles.addOne(e, counter)
                  case e => null
                }
            }
            Thread.sleep(1000)

            if (scUpdatedBattles.isEmpty) {
              dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
              defaultSchedule()
            }
            else {
              Await.ready(saveToGameHistory(scUpdatedBattles.toSeq), Duration.Inf)
              Await.ready(insertOrUpdateSystemProcess(scUpdatedBattles.toSeq), Duration.Inf)
              // wait prev txs finished and removed SC Battle Counter
              scUpdatedBattles.clear()

              for {
                // clean back the database table for characters.
                _ <- ghostQuestCharacterService.removeAllGhostQuestCharacter
                // get all characters on smartcontract, ready to save into DB..
                updatedCharacters <- {
                  ghostQuestGameService.getAllCharacters.map(_.map(_.map(_.game_data.characters).flatten))
                }
                // fetch smartcontract with updated data
                _ <- Future.successful {
                  updatedCharacters.map { characters =>
                    for {
                      // filter characters that has no life
                      noLifeCharacters <- Future.successful(characters.filter(_.value.character_life == 0))
                      // remove from smartcontract and successful removed
                      // will be added to character history
                      _ <- Future.successful {
                        noLifeCharacters.map { character =>
                          ghostQuestGameService.eliminate(character.value.owner_id, character.key).map {
                            case Some(hash) =>
                              ghostQuestCharacterService.insertGhostQuestCharacterHistory(character.toHistory())
                            case None => ()
                          }
                        }
                      }
                    } yield ()
                  }
                }
              } yield (defaultSchedule())
            }
          }
        }
      } yield ()

		case _ => ()
	}
  // challengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double)
  private def insertOrUpdateSystemProcess(seq: Seq[(String, (UUID, GhostQuestBattleResult))]): Future[Seq[Unit]] = Future.successful {
    seq.map { case (hash, (gameID, result)) =>
      result.characters.map { v =>
        val gameID: Int = v._2._1
        // find account by gameID
        userAccountService
          .getAccountByGameID(gameID)
          .map(_.map { acc =>
            dynamicProcessor ! DailyTask(acc.id, GQ_GAME_ID, 1)
            dynamicProcessor ! ChallengeTracker(acc.id, 1, (if(v._2._2) 2 else 0), 1, (if(v._2._2) 1 else 0))
          })
      }
    }
  }

  private def saveToGameHistory(data: Seq[(String, (UUID, GhostQuestBattleResult))]): Future[Seq[Any]] = Future.sequence {
    data.map { case (txHash, (gameID, result)) =>
      val winner = result.characters.filter(_._2._2).head
      val loser = result.characters.filter(!_._2._2).head
      val time = Instant.now.getEpochSecond

      Await.ready(
        for {
          winnerAcc <- userAccountService.getAccountByGameID(winner._2._1)
          loserAcc <- userAccountService.getAccountByGameID(loser._2._1)
        } yield ((new OverAllGameHistory(
                              UUID.randomUUID,
                              txHash,
                              gameID.toString,
                              GQ_CODE,
                              BooleanPredictions(winnerAcc.map(_.username).getOrElse(""), true, true, 1, 1, None),
                              true,
                              time),
          new OverAllGameHistory(
                              UUID.randomUUID,
                              txHash,
                              gameID.toString,
                              GQ_CODE,
                              BooleanPredictions(loserAcc.map(_.username).getOrElse(""), true, false, 1, 0, None),
                              true,
                              time)),
          new GhostQuestCharacterGameHistory(
                        gameID.toString, // Game ID
                        txHash,
                        winner._2._1,
                        winner._1,
                        loser._2._1,
                        loser._1,
                        result.logs,
                        time)), Duration.Inf)
      .map {
        case ((winner, loser), character) =>
          // insert Tx and character contineously
          // broadcast game result to connected users
          // use live data to feed on history update..
          Await.ready(for {
            _ <- ghostQuestCharacterService.insertGameHistory(character)
            _ <- historyService.addOverAllHistory(winner)
            _ <- historyService.addOverAllHistory(loser)
          } yield (), Duration.Inf)
          // broadcast GQ game result
          // Thread.sleep(1000)
          dynamicBroadcast ! Array(winner, loser)
        case _ => ()
      }
    }
  }

  private def battleProcess(characters: HashMap[String, GhostQuestCharacter]):
    Future[(HashMap[String, GhostQuestCharacterValue], HashMap[UUID, GhostQuestBattleResult])] = Future.successful {
    val noEnemy = HashMap.empty[String, GhostQuestCharacterValue]
    val battleCounter = HashMap.empty[UUID, GhostQuestBattleResult]
    do {
      val player: (String, GhostQuestCharacter) = characters.head

      if (characters.size == 1) {
        characters.remove(player._1)
        noEnemy.addOne(player._1, player._2.value)
      }
      else {
        val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
        // set date range for characters that can battle again each other...
        val filteredDateForBattle: Instant = now.plusDays(-7).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        // remove his other owned characters from the list
        // check chracters spicific history to avoid battling again as posible..
        for {
          filterNotOwned <- Future.successful(characters.filter(_._2.value.owner_id != player._2.value.owner_id))
          characterHistory <- {
            ghostQuestCharacterService.getGameHistoryByUsernameCharacterIDAndDate(
                                        player._1,
                                        player._2.value.owner_id,
                                        filteredDateForBattle.getEpochSecond,
                                        now.toInstant(ZoneOffset.UTC).getEpochSecond)
          }
          // remove played characters from the current history result
          finalCharactersToBattle <- Future.successful {
            filterNotOwned
              .filterNot(ch => characterHistory.map(_.loserID).contains(ch._1))
              .filterNot(ch => characterHistory.map(_.winnerID).contains(ch._1))
          }
          _ <- Future.successful {
            if (!finalCharactersToBattle.isEmpty && battleCounter.filter(_._2.characters.map(_._1).toSeq.contains(player._1)).isEmpty) {
              val enemy: (String, GhostQuestCharacter) = finalCharactersToBattle.head
              // make sure battle of characters are not yet exists in GQBattleScheduler.battleCounter
              if (battleCounter.filter(_._2.characters.map(_._1).toSeq.contains(enemy._1)).isEmpty) {
                val battle = new GhostQuestBattleCalculation[GhostQuestCharacter](player._2, enemy._2)
                // save result into battleCounter, if failed save into noEenmy
                if (battle.result.equals(None) || battle.result.map(_.characters.size).getOrElse(0) < 2) {
                  noEnemy.addOne(player._1, player._2.value)
                  noEnemy.addOne(enemy._1, enemy._2.value)
                }
                else battle.result.map(x => battleCounter.addOne(x.id, x))
              }
              else {
                noEnemy.addOne(player._1, player._2.value)
                noEnemy.addOne(enemy._1, enemy._2.value)
              }
              // remove both characters into the current battle processs..
              characters.remove(player._1)
              characters.remove(enemy._1)
            }
            else {
              characters.remove(player._1)
              noEnemy.addOne(player._1, player._2.value)
            }
          }
        } yield ()
      }

    } while (!characters.isEmpty)

    (noEnemy, battleCounter)
  }

  private def systemBattleScheduler(timer: FiniteDuration): Unit = {
    system.scheduler.scheduleOnce(timer) {
      println("GQ BattleScheduler Starting")
      GhostQuestSchedulerActor.nextBattle = 0
      self ! "REQUEST_BATTLE_NOW"
    }
  }
  private def defaultSchedule(): Unit = {
    GhostQuestSchedulerActor.nextBattle = Instant.now().getEpochSecond + (60 * GhostQuestSchedulerActor.defaultTimeSet)
    systemBattleScheduler(GhostQuestSchedulerActor.scheduledTime)
  }
}