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
      gQGameHistoryRepo: GQCharacterGameHistoryRepo,
      support: EOSIOSupport,
      eosio: GQSmartContractAPI)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  // private var isIntialized: Boolean = false
  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // make sure all wallets are locked to avoid tx error..
    // support.lockAllWallets()
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
                if (!isExists) {
                  characterRepo.insert(info)
                  println("  "+ info.owner + " ~> " + info.id + " INSERTED")
                }
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

        _ <- {
          // 2 History with same data but difference only on player fields and
          // merge it and use only 1 entry in DB for same history in 2 players..
          // val groupedByGameID = characterPrevMatches.groupBy(_._1._2)
          val groupedByGameID = characterPrevMatches.groupBy(_._2.key)
          // iterate each group
          val seqOfGameHistory = groupedByGameID.map { case (gameid, players) =>
            // check if character still available or already been eliminated
            if (players.size > 1) {
              // create game history out of 2 player's history..
              val player1: ((String, String), GQCharacterPrevMatch) = players(0)
              val player2: ((String, String), GQCharacterPrevMatch) = players(1)

              new GQCharacterGameHistory(
                gameid,
                player1._1._1,
                player1._1._2,
                player2._1._1,
                player2._1._2,
                player1._2.value.time_executed,
                player1._2.value.gameplay_log,
                List(new GQGameStatus(player1._1._1, player1._2.value.isWin),
                    new GQGameStatus(player2._1._1, player2._2.value.isWin)))
            }
            else null
          }

          Future.sequence(
            seqOfGameHistory
              .filterNot(_ == null)
              .map { game =>
              // check if game already exists on history DB..
              for {
                isExist <- gQGameHistoryRepo.exist(game.id)
                isAdded <- if (!isExist) gQGameHistoryRepo.insert(game).map(_ => true) else Future(false)
              } yield (isAdded)
          })
          .map { isUpdated =>
            if(isUpdated.forall(_ == true))
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
      for  {
        _ <- Future(support.unlockWalletAPI()).map(_ => defaultThreadSleep())
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
                  val filtered:Seq[GQCharacterData] = response.filter(_.character_life > 0)
                  val characters: Seq[GQCharacterData] = scala.util.Random.shuffle(filtered)
                  val currentlyPlayed = new HashMap[String, String]() // chracter_ID and player_name

                  characters.foreach { character =>
                    // println(character.owner)
                    // remove his own characters from the list
                    // all characters from other players...(DONE)
                    val removedOwnCharacters: Seq[GQCharacterData] =
                      characters.filterNot(_.owner.equals(character.owner))

                    // get all history related to this character
                    val gameHistories: Seq[GQCharacterGameHistory] =
                      gameHistory.filter(_.id == character.id)

                    // remove played characters from gameHistories DB
                    val checkedHistoryDB: Seq[GQCharacterData] =
                      removedOwnCharacters.filterNot(ch => gameHistories.map(_.player2ID).contains(ch.id))

                    // remove played character on current characters list from currentlyPlayed.. (ON TEST)
                    val checkedCurrentCycle: Seq[GQCharacterData] =
                      checkedHistoryDB.filterNot(ch => currentlyPlayed.map(_._1).toSeq.contains(ch.id))

                    if (!checkedCurrentCycle.isEmpty) {
                       try {
                        val enemy: GQCharacterData = checkedCurrentCycle.head
                        val req: Seq[(String, String)] = Seq(
                          (character.id.toString, character.owner.toString),
                          (enemy.id.toString, enemy.owner.toString))

                        // enhancements needed here ..
                        // issue with battle action succesful but returns error in the server
                        eosio.battleAction(req, UUID.randomUUID()).map {
                          // add to finished list after succesful battle ACTION..
                          // and remove both characters in the current game rotation..
                          case Some(e) =>
                            currentlyPlayed(character.id) = character.owner
                            currentlyPlayed(enemy.id) = enemy.owner

                          case None =>
                            println("Error: Battle " + character.id + " ~~> " + enemy.id)
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

          _ <- Future(self ! VerifyGQUserTable(new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)))
      } yield ()

    case RemoveCharacterWithNoLife =>
      // unlock your wallet..
      for {
        // _ <- Future(support.unlockWalletAPI())
        _ <- {
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
                              characterRepo.insertHistory(GQCharacterDataHistory(ch))
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
            // support.lockAllWallets()
          }
        }
        _ <- Future(support.lockAllWallets())
      } yield ()

    case VerifyGQUserTable(request) =>
      eosio.getGQUsers(request).map(_.map(self ! _))

    case e =>
      println("Error: Unkown data received")
  }

  def defaultThreadSleep(): Unit = Thread.sleep(1500)
}