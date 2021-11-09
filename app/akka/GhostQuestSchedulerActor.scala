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
import utils.GameConfig._

object GhostQuestSchedulerActor {
  val defaultTimeSet: Int            = GQ_DEFAULT_BATTLE_TIMER
  val scheduledTime : FiniteDuration = { defaultTimeSet }.minutes
  var isIntialized  : Boolean        = false
  var nextBattle    : Long           = 0
  val noEnemy: HashMap[String, GhostQuestCharacterValue] = HashMap.empty[String, GhostQuestCharacterValue]
  val scUpdatedBattles = HashMap.empty[String, GhostQuestBattleResult]
  def props(historyService: HistoryService,
            userAccountService: UserAccountService,
            characterService: GhostQuestCharacterService,
            gameService: GhostQuestGameService)(implicit system: ActorSystem) =
    Props(classOf[GhostQuestSchedulerActor],
          historyService,
          userAccountService,
          characterService,
          gameService,
          system)
}

@Singleton
class GhostQuestSchedulerActor @Inject()(
      historyService: HistoryService,
      userAccountService: UserAccountService,
      characterService: GhostQuestCharacterService,
      gameService: GhostQuestGameService,
      @Named("DynamicBroadcastActor") dynamicBroadcast: ActorRef,
      @Named("DynamicSystemProcessActor") dynamicProcessor: ActorRef,
    )(implicit system: ActorSystem ) extends Actor with ActorLogging {
  implicit private val timeout: Timeout = new Timeout(5, concurrent.TimeUnit.SECONDS)

  override def preStart: Unit = {
    super.preStart
    // keep alive connection
    // akka.stream.scaladsl.Source.tick(0.seconds, 2.minutes, "GhostQuestSchedulerActor").runForeach(n => ())
    system.actorSelection("/user/GhostQuestSchedulerActor").resolveOne().onComplete {
      case Success(actor) =>
        if (!GhostQuestSchedulerActor.isIntialized) {
          GhostQuestSchedulerActor.isIntialized = true

          GhostQuestSchedulerActor.nextBattle = Instant.now().getEpochSecond + (60 * GhostQuestSchedulerActor.defaultTimeSet)
          systemBattleScheduler(GhostQuestSchedulerActor.scheduledTime)
          log.info("GQ Scheduler Actor Initialized")
        }
      case Failure(ex) => // if actor is not yet created do nothing..
    }
  }

  def receive: Receive = {
  	case "REQUEST_BATTLE_NOW" =>
      println("REQUEST_BATTLE_NOW")
      for {
        _ <- Await.ready(characterService.removeAllGhostQuestCharacter, Duration.Inf)
        _ <- Await.ready(gameService.removeAllBattleResult, Duration.Inf)
        // get all characters on smartcontract
        // flatten Seq[Seq[]] to Seq[] and ready to save into DB..
        extractCharacters <- Await.ready(gameService.getAllCharacters.map(_.map(_.map(_.game_data.characters).flatten)), Duration.Inf)
        _ <- Future.sequence(extractCharacters.map(x => characterService.insertGhostQuestCharacters(x)).toSeq)
        // get save characters from DB
        availableCharacters <- characterService.allGhostQuestCharacter()
        // remove characters that reach battle limit
        hasReachLimit <- Future.successful(availableCharacters.filter(x => x.value.battle_count < x.value.battle_limit))
        // convert availableCharacters into HashMap[String, GhostQuestCharacter]
        hashMapCharacters <- Future.successful(HashMap(hasReachLimit.map(ch => (ch.key, ch)) : _*))
        _ <- Await.ready(battleProcess(hashMapCharacters), Duration.Inf)
        // broadcast characters no available enemy..
        _ <- Future.successful(dynamicBroadcast ! ("BROADCAST_CHARACTER_NO_ENEMY", GhostQuestSchedulerActor.noEnemy.groupBy(_._2)))
        // get all battle recorded on the DB
        battles <- gameService.getAllBattleResult()
        _ <- Await.ready(Future.sequence {
          battles.map { counter =>
            val winner = counter.characters.filter(_._2._2).head
            val loser = counter.characters.filter(!_._2._2).head

            Await.ready(gameService.battleResult(counter.id.toString, (winner._1, winner._2._1), (loser._1, loser._2._1)).map {
              case Some(e) => GhostQuestSchedulerActor.scUpdatedBattles.addOne(e, counter)
              case e => ()
            }, Duration.Inf)
          }
        }, Duration.Inf)
        _ <- Await.ready(Future {
          if (battles.isEmpty || GhostQuestSchedulerActor.scUpdatedBattles.isEmpty)
            dynamicBroadcast ! "BROADCAST_NO_CHARACTERS_AVAILABLE"
          Thread.sleep(5000)
        }, Duration.Inf)
        _ <- Await.ready(insertOrUpdateSystemProcess(), Duration.Inf)
        _ <- Await.ready(saveToGameHistory(), Duration.Inf)
        // to make sure no characters without life will be removed on the next battle
        _ <- removeNoLifeCharacters()
        _ <- Future(GhostQuestSchedulerActor.scUpdatedBattles.clear())
      } yield (defaultSchedule())
		case _ => ()
	}
  // challengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double)
  private def insertOrUpdateSystemProcess(): Future[Seq[Unit]] = Future.successful {
    GhostQuestSchedulerActor.scUpdatedBattles.toSeq.map { case (hash, result) =>
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
  // returns Option[Future[Seq[Future[Int]]]]
  private def removeNoLifeCharacters() = {
    for {
      // clean back the database table for characters.
      _ <- characterService.removeAllGhostQuestCharacter
      // get all characters on smartcontract, ready to save into DB..
      updatedCharacters <- {
        gameService.getAllCharacters.map(_.map(_.map(_.game_data.characters).flatten))
      }
      // fetch smartcontract with updated data
      updatedContract <- Future.successful {
        updatedCharacters.map { characters =>
          for {
            // filter characters that has no life
            noLifeCharacters <- Future.successful(characters.filter(_.value.character_life == 0))
            // remove from smartcontract and successful removed
            // will be added to character history
            removed <- Future.sequence {
              noLifeCharacters.map { character =>
                gameService.eliminate(character.value.owner_id, character.key).map {
                  case Some(hash) =>
                    characterService.insertGhostQuestCharacterHistory(character.toHistory())
                  case None => Future(0)
                }
              }
            }
          } yield (removed)
        }
      }
    } yield (updatedContract)
  }

  private def saveToGameHistory(): Future[Seq[Any]] = Future.sequence {
    println("saveToGameHistory", GhostQuestSchedulerActor.scUpdatedBattles.toSeq.size)
    // Aadd delay to make sure that past process are finished
    Thread.sleep(3000)
    GhostQuestSchedulerActor.scUpdatedBattles.toSeq.map { case (txHash, result) =>
      val gameID = result.id
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
          println(winner.id, loser.id)
          // insert Tx and character contineously
          // broadcast game result to connected users
          // use live data to feed on history update..
          for {
            _ <- Await.ready(characterService.insertGameHistory(character), Duration.Inf)
            _ <- Await.ready(historyService.addOverAllHistory(winner), Duration.Inf)
            _ <- Await.ready(historyService.addOverAllHistory(loser), Duration.Inf)
          } yield ()
          // broadcast GQ game result
          // Thread.sleep(1000)
          dynamicBroadcast ! Array(winner, loser)
        case _ => ()
      }
    }
  }
  private def battleProcess(v: HashMap[String, GhostQuestCharacter]): Future[Unit] = Future {
    val characters: HashMap[String, GhostQuestCharacter] = v
    // val battleCounter = HashMap.empty[UUID, GhostQuestBattleResult]
    do {
      val player: (String, GhostQuestCharacter) = characters.head
      if (characters.size == 1) {
        characters.remove(player._1)
        GhostQuestSchedulerActor.noEnemy.addOne(player._1, player._2.value)
      }
      else {
        val now: LocalDateTime = LocalDateTime.ofInstant(Instant.now, ZoneOffset.UTC)
        // set date range for characters that can battle again each other...
        val filteredDateForBattle: Instant = now.plusDays(-7).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        // remove his other owned characters from the list
        // check chracters spicific history to avoid battling again as posible..
        Await.ready(for {
          filterNotOwned <- Future.successful(characters.filter(_._2.value.owner_id != player._2.value.owner_id))
          characterHistory <- {
            characterService.getGameHistoryByUsernameCharacterIDAndDate(
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
          battles <- gameService.getAllBattleResult
          _ <- Future.successful {
            // fetch all characters on the DB and find if character already exists...
            if (!finalCharactersToBattle.isEmpty && battles.filter(_.characters.map(_._1).toSeq.contains(player._1)).isEmpty) {
              val enemy: (String, GhostQuestCharacter) = finalCharactersToBattle.head
              // make sure battle of characters are not yet exists in GQBattleScheduler.battleCounter
              val battle = new GhostQuestGameplay[GhostQuestCharacter](player._2, enemy._2)
              // save result into battleCounter, if failed save into noEenmy
              // println(battle.result.equals(None))
              // println(battle.result.map(_.characters.size).getOrElse(0) < 2)
              if (battle.result.equals(None) || battle.result.map(_.characters.size).getOrElse(0) < 2)
                GhostQuestSchedulerActor.noEnemy ++= List(player._1 -> player._2.value, enemy._1 -> enemy._2.value)
              // insert into battle result table..
              else battle.result.map(gameService.insertBattleResult(_))
              // remove both characters into the current battle processs..
              characters.remove(player._1)
              characters.remove(enemy._1)
            }
            else {
              characters.remove(player._1)
              GhostQuestSchedulerActor.noEnemy.addOne(player._1, player._2.value)
            }
          }
        } yield (), Duration.Inf)
      }
    } while (!characters.isEmpty);
    // (noEnemy)
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