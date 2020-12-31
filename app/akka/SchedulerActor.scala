package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.collection.mutable.{ ListBuffer, HashMap }
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import models.domain.eosio._
import models.repo.eosio._
import models.service.GQSmartContractAPI
import akka.domain.common.objects._
import utils.lib.EOSIOSupport

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(
      characterRepo: GQCharacterDataRepo,
      gQCDHistoryRepo: GQCharacterDataHistoryRepo,
      gQGameHistoryRepo: GQCharacterGameHistoryRepo,
      support: EOSIOSupport,
      eosio: GQSmartContractAPI)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  // private var isIntialized: Boolean = false
  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // make sure all wallets are locked to avoid tx error..
    support.lockAllWallets()
    // load GhostQuest users to DB update for one time.. in case server is down..
    val eosTblRowsRequest: TableRowsRequest = new TableRowsRequest(
                                                  "ghostquest",
                                                  "users",
                                                  "ghostquest",
                                                  None,
                                                  Some("uint64_t"),
                                                  None,
                                                  None,
                                                  None)
    self ! VerifyGQUserTable(eosTblRowsRequest)

    // scheduled on every 5 minutes
    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 5.minute, interval = 5.minute)(() => self ! BattleScheduler)

    // scheduled on every 1 hr to verify data integrity..
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.hour, interval = 1.hour)(() => self ! VerifyGQUserTable(eosTblRowsRequest))
  }

  def receive: Receive = {
    case connect: Connect =>
      // Indicators if there's new Client Connected..
      // println(connect)

    case GQRowsResponse(rows, hasNext, nextKey) =>
      val seqCharacters = ListBuffer[GQCharacterData]()
      val characterPrevMatches = ListBuffer[((String, String), GQCharacterPrevMatch)]()
      for {
        _ <- Future.successful { // process data
          rows.foreach { row =>
            val username: String = row.username
            val gameData: GQGame = row.game_data

            gameData.character.foreach { ch =>
              val chracterInfo = new GQCharacterData(
                                    ch.key,
                                    ch.value.owner,
                                    ch.value.character_life,
                                    ch.value.initial_hp,
                                    ch.value.ghost_class,
                                    ch.value.ghost_level,
                                    ch.value.status,
                                    ch.value.attack,
                                    ch.value.defense,
                                    ch.value.speed,
                                    ch.value.luck,
                                    ch.value.prize,
                                    ch.value.battle_limit,
                                    ch.value.battle_count,
                                    ch.value.last_match)

              seqCharacters.append(chracterInfo)

              ch.value.match_history.foreach(mtch => {
                characterPrevMatches.append(((username, ch.key), mtch))
              })
            }
          }
        }

        _ <-  Future.successful { // insert Seq[GQCharacterData]
          println("\n============\tValidating Characters    ============\n")
          seqCharacters.map { info =>
            try { // check if data aleady exists
              characterRepo.find(info.owner, info.id).map { isExists =>
                if (!isExists)
                  characterRepo.insert(info)
                else {
                  characterRepo.update(info).map { update =>
                    if (update > 0)
                      println("  "+ info.owner + " ~> " + info.id + " UPDATED")
                    else
                      println("  "+ info.owner + " ~> " + info.id + " FAILED TO UPDATE")
                  }
                }
              }
            } catch { // TODO: save failed insertion data..
              case e: Throwable => println(e)
            }

            defaultThreadSleep() // help prevent from overlapping existing process..
          }
        }

        _ <- Future.successful { // convert to GQCharacterGameHistory and insert Seq[GQCharacterPrevMatch]
          val setOfCharactersGameHistory: ListBuffer[GQCharacterGameHistory] = characterPrevMatches.map({
            case ((owner, id), info) => new GQCharacterGameHistory(
                                            UUID.randomUUID(),
                                            info.key,
                                            owner,
                                            info.value.enemy,
                                            id,
                                            info.value.enemy_id,
                                            info.value.time_executed,
                                            info.value.gameplay_log,
                                            info.value.isWin)})

          Future.sequence(setOfCharactersGameHistory.map{ history =>
            gQGameHistoryRepo
              .existByID(history.game_id, history.player_id)
              .map(isExists => if (!isExists) gQGameHistoryRepo.insert(history).map(_ => true) else Future(false))
              .flatten
          }).map { x =>
            if(x.forall(_ == true))
              println("\n============\tHistory has been updated ============\n")
            else
              println("\n============\tHistory is up to date    ============\n")
          }
        }

        _ <- Future.successful {
          // check if the first result still hasnext data
          if (hasNext)
            self ! VerifyGQUserTable(new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, Some(nextKey)))
          // remove eliminated chracter from users table..
          else
            self ! RemoveCharacterWithNoLife
        }
      } yield ()

    case BattleScheduler =>
      // Note: no more chracters that are eliminated in the game..
      // unlock wallet to execute the battle command...
      for  {
        _ <- Future.successful(support.unlockWalletAPI().map(_ => defaultThreadSleep()))

        // get latest history data..
        gameHistory <- gQGameHistoryRepo.all()

        _ <- characterRepo
              .all()
              .map { response =>
                if (response.isEmpty || response.size == 1)
                  println("No available characters.")
                else {
                  // loop chracters in the database
                  // character of the loop is the player (current index)
                  // and the rest will serve as the enemy..
                  val characters: Seq[GQCharacterData] = scala.util.Random.shuffle(response).filter(_.character_life > 0)
                  val currentlyPlayed = new HashMap[String, String]() // chracter_ID and player_name

                  characters.foreach { character =>
                    // println(character.owner)
                    // remove his own characters from the list
                    // all characters from other players...(DONE)
                    val removedOwnCharacters: Seq[GQCharacterData] =
                      characters.filterNot(_.owner.equals(character.owner))

                    // get all history related to this character.. (DONE)
                    val gameHistories: Seq[GQCharacterGameHistory] =
                      gameHistory.filter(_.player_id == character.id)

                    // remove played characters from gameHistories DB.. (DONE)
                    val checkedHistoryDB: Seq[GQCharacterData] =
                      removedOwnCharacters.filterNot(ch => gameHistories.map(_.enemy_id).contains(ch.id))

                    // remove played character on current characters list from currentlyPlayed.. (ON TEST)
                    val checkedCurrentCycle: Seq[GQCharacterData] =
                      checkedHistoryDB.filterNot(ch => currentlyPlayed.map(_._1).toSeq.contains(ch.id))

                    if (!checkedCurrentCycle.isEmpty) {
                       try {
                        val enemy: GQCharacterData = checkedCurrentCycle.head
                        val req: Seq[(String, String)] = Seq(
                          (character.id.toString, character.owner.toString),
                          (enemy.id.toString, enemy.owner.toString))

                        eosio.battleAction(req, UUID.randomUUID()).map {
                          case Some(e) =>
                            // add to finished list after succesful battle ACTION..
                            // and remove both characters in the current game rotation..
                            currentlyPlayed(character.id) = character.owner
                            currentlyPlayed(enemy.id) = enemy.owner

                          case None => println("Error: Battle " + character.id + " ~~> " + enemy.id)
                        }
                      }
                      catch { case e: Throwable => println("Error: System is not responding.") }
                    } else {
                      println("No available enemy for ~~> " + character.id)
                      currentlyPlayed(character.id) = character.owner
                    }

                    defaultThreadSleep()
                  }
                }
              }

          _ <- Future.successful(support.lockAllWallets())
          // update database ..
          _ <- Future.successful(self ! VerifyGQUserTable(new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)))
      } yield ()

    case RemoveCharacterWithNoLife =>
      // unlock your wallet..
      support.unlockWalletAPI().map(_ => defaultThreadSleep())

      // get all characters that has no life on DB
      characterRepo.getNoLifeCharacters.map { characters =>
        if (characters.size > 0)
          for {
            // remove characters that has no life on the contract..
            _ <- Future.successful {
              characters.foreach { ch =>
                eosio.removeCharacter(ch.owner, ch.id).map {
                  // Remove from the Character Data DB
                  case Some(x) =>
                    for {
                      isDeleted <- characterRepo.remove(ch.owner, ch.id)
                      // check if Successfully removed the add to game data history..
                      _ <-  {
                        if (isDeleted > 0)
                          gQCDHistoryRepo.insert(GQCharacterDataHistory.fromCharacterData(ch))
                        else
                          Future(None)
                      }
                      // TODO: check the reason why it is failed..
                    } yield ()

                  // TODO: check the reason why it is failed..
                  case None => println("Error: removing character")
                }
                // add timeout for contract response..
               defaultThreadSleep()
              }
              println("All Characters with no life has been removed")
            }
          } yield ()

        else
          println("Info: There's no eliminated character/s to remove in the DB.")
        // close wallet after using...
        support.lockAllWallets()
      }

    case VerifyGQUserTable(request) =>
      eosio.getGQUsers(request).map(_.map(self ! _))

    case e =>
      println("Error: Unkown data received")
  }

  def defaultThreadSleep(): Unit = Thread.sleep(2000)
}