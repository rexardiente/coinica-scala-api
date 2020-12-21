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
    val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    self ! VerifyGQUserTable(req)

    
    // scheduled on every 3 minutes
    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => self ! BattleScheduler)

    // scheduled on every 1 hr to verify data integrity..
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => {
    //   self ! VerifyGQUserTable(req)})

    // self ! BattleScheduler
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
              val chracterInfo: GQCharacterData = new GQCharacterData(
                  ch.key,
                  ch.value.owner,
                  ch.value.character_life,
                  ch.value.initial_hp,
                  ch.value.hitpoints,
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

            Thread.sleep(200) // help prevent from overlapping existing process.. 
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
                if (response.isEmpty)
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
                    val playedHistory: Seq[GQCharacterGameHistory] = 
                      gameHistory.filter(_.player_id == character.id)
                    
                    // remove played characters from playedHistory DB.. (DONE)
                    val removedPlayedHistoryDB: Seq[GQCharacterData] = 
                      removedOwnCharacters.filterNot(enemy => playedHistory.map(_.enemy_id).contains(enemy.id))

                    // remove played character on current characters list from currentlyPlayed.. (ON TEST)
                    val removedPlayed: Seq[GQCharacterData] = 
                      removedPlayedHistoryDB.filterNot(enemy => currentlyPlayed.map(_._1).toSeq.contains(enemy.id))
                      

                    if (removedPlayed.isEmpty) {
                      println("No available enemy for ~~> " + character.id)
                      currentlyPlayed(character.id) = character.owner
                    }
                    else {
                       try {
                        val enemy: GQCharacterData = removedPlayed.head
                        val req: Seq[(String, String)] = Seq(
                          (character.id.toString, character.owner.toString), 
                          (enemy.id.toString, enemy.owner.toString))

                        eosio.battleAction(req, UUID.randomUUID()).map {
                          case Left(x) =>
                            println(x)
                            println("Error: Battle " + character.id + " ~~> " + enemy.id)
                          // add to finished list after succesful battle ACTION..
                          case Right(e) =>
                            // remmove both characters in the current game rotation..
                            currentlyPlayed(character.id) = character.owner
                            currentlyPlayed(enemy.id) = enemy.owner
                        }
                      } catch {
                        case e: Throwable => 
                          println("Error: System is not responding.")
                      }
                    }

                    defaultThreadSleep()
                  }
                }
              }

          _ <- Future.successful(support.lockAllWallets())
          // update database ..
          _ <- Future.successful {
            val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
            self ! VerifyGQUserTable(req)
          }
      } yield ()

    case RemoveCharacterWithNoLife =>
      // get all characters that has no life on DB
      characterRepo.getNoLifeCharacters.map { characters =>
        if (characters.size > 0)
          for {
            // unlock your wallet..
            _ <- Future.successful(support.unlockWalletAPI().map(_ => defaultThreadSleep()))
            // remove characters that has no life on the contract..
            _ <- Future.successful {
              characters.foreach { ch =>
                eosio.removeCharacter(ch.owner, ch.id).map {
                  // TODO: check the reason why it is failed..  
                  case Left(x) => 
                    println("Error: removing character")
                  // Remove from the Character Data DB
                  case Right(x) =>
                    for {
                      isDeleted <- characterRepo.remove(ch.owner, ch.id)
                      playCount <- gQGameHistoryRepo.getSize(ch.id, ch.owner)
                      // check if Successfully removed the add to game data history..
                      _ <- gQCDHistoryRepo.insert(GQCharacterDataHistory.fromCharacterData(ch, playCount))
                      // _ <- Future.successful {
                        // insert into game data history
                        // if (playCount > 0)
                        //   gQCDHistoryRepo.insert(GQCharacterDataHistory.fromCharacterData(ch, playCount))
                        // // TODO: check the reason why it is failed..  
                        // else
                        //   gQCDHistoryRepo.insert(GQCharacterDataHistory.fromCharacterData(ch, 0)) // return nothing
                      // }
                    } yield ()
                }
                // add timeout for contract response..
               defaultThreadSleep()
              }
              println("All Characters with no life has been removed")
            }
            _ <- Future.successful(support.lockAllWallets())
          } yield ()
        
        else 
          println("Info: There's no eliminated character/s to remove in the DB.")
      }

    case VerifyGQUserTable(request) =>
      eosio.getGQUsers(request).map(_.map(self ! _))

    case e => 
      println("Error: Unkown data received")
  }

  def defaultThreadSleep(): Unit = Thread.sleep(2000)
}