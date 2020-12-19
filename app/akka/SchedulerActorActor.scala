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
import models.service.SmartContractAPI
import akka.domain.common.objects._

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(
      eosio: SmartContractAPI,
      characterRepo: GQCharacterDataRepo, 
      gQCDHistoryRepo: GQCharacterDataHistoryRepo, 
      gQGameHistoryRepo: GQCharacterGameHistoryRepo)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // load GhostQuest users to DB update for one time.. in case server is down..
    // val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    // self ! LoadGQUserTable(req)
    // scheduled on every 3 minutes
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.minute, interval = 10.minute)(() => self ! BattleScheduler)
    // scheduled on every 1 hr
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.hour, interval = 1.hour)(() => self ! LoadGQUserTable(req))
  }

  def receive: Receive = {
    case connect: Connect => 
      // Indicators if there's new Client Connected..
      // println(connect)

    case GQRowsResponse(rows, hasNext, nextKey) =>
      val seqCharacters = ListBuffer[GQCharacterData]()
      val characterPrevMatches = ListBuffer[((String, String), Seq[GQCharacterPrevMatch])]()

      for {
        _ <- Some({ // process data
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
                characterPrevMatches.append(((username, ch.key), ch.value.match_history))
              })
            }
          }
        })

        _ <-  Some { // insert Seq[GQCharacterData] 
          seqCharacters.map { info =>
            try { // check if data aleady exists
              characterRepo.find(info.owner, info.id).map { isExists =>
                if (!isExists) 
                  characterRepo.insert(info)
                else {
                  characterRepo.update(info).map { update =>
                    if (update > 0) 
                      println("Update Successful: " + info.owner.toUpperCase + " ~> character " + info.id)
                    else
                      println("Update Failed: " + info.owner.toUpperCase + " ~> character " + info.id)
                  }
                }
              }
            } catch { // TODO: save failed insertion data..
              case e: Throwable => println(e)
            }
          }
        }

        _ <- Some { // convert to GQCharacterGameHistory and insert Seq[GQCharacterPrevMatch]
          val setOfCharactersGameHistory: ListBuffer[GQCharacterGameHistory] = characterPrevMatches.map({ 
            case ((owner, id), seq) =>
              seq.map({ x => 
                new GQCharacterGameHistory(
                UUID.randomUUID(), 
                x.key, 
                owner, 
                x.value.enemy, 
                id, 
                x.value.enemy_id,
                x.value.time_executed, 
                x.value.gameplay_log, 
                x.value.isWin)
              })}).flatten

          setOfCharactersGameHistory.foreach(history => {
            gQGameHistoryRepo.exist(history.game_id, history.player).map { isExists =>
              if (!isExists) gQGameHistoryRepo.insert(history)
              // else {
              //   gQGameHistoryRepo.update(history).map { update =>
              //     if (update > 0) 
              //       println("STATUS UPDATE SUCCESS: " + info.owner.toUpperCase + " ~> character " + info.key)
              //     else
              //       println("STATUS UPDATE FAILED: " + info.owner.toUpperCase + " ~> character " + info.key)
              //   }
              // }
            }
          })
        }
      } yield ()

      // check if the first result still hasnext data
      if (hasNext)
        self ! LoadGQUserTable(new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, Some(nextKey)))
      // remove eliminated chracter from users table..   
      else self ! RemoveCharacterWithNoLife


    case BattleScheduler => 
      // Note: no more chracters that are eliminated in the game..
      // unlock wallet to execute the battle command...
      for  {
      // _ <- Future(eosio.unlockWalletAPI())

      _ <- Future.successful {
        characterRepo
          .all()
          .map { characters =>
            // shuffled chracters list
            val shuffled: Seq[GQCharacterData] = scala.util.Random.shuffle(characters).filter(_.character_life > 0)
            
            // convert Seq[chracters] to HashMap for Done playing or no available to play...
            val finished = new HashMap[String, String]() // chracter ID

            // loop all chracters to find available enemy and do the battle...
            shuffled.map { character =>
              // filter list of chracter that is not yet fought before..
              // fetch DB to check characters game history..

              // list of played enemy ~~> returns player and ID
              val played: Future[Seq[(String, String)]] =
                gQGameHistoryRepo
                  .find(character.id, character.owner)
                  .map(_.map(res => (res.enemy, res.enemy_id)).distinct)

              // remove his characters from the list and
              // convert to hashMap for faster validations
              val availableCharacters = new HashMap[(String, String), GQCharacterData]()
              characters
                .filterNot(_.owner == character.owner)
                .map(ch => availableCharacters((ch.owner, ch.id)) = ch)

              finished.map { donePlayed =>
                availableCharacters.remove((donePlayed._2, donePlayed._1))
              }

              // remove already played characters from availableCharacters..
              played.map(_.foreach(availableCharacters.remove(_)))

              // check if there are still remaining characters to play..
              if (availableCharacters.isEmpty) {
                println("No available enemy for ~~> " + character.id)
                finished(character.id) = character.owner
              }

              // if empty remove the user from available characters..
              // hashMapCharacters.remove((character.id, character.owner))
              else {
                // make sure player hasnt played already...
                if (finished.find(x => x._1 == character.id && x._2 == character.owner).map(_ => true).getOrElse(false)) {
                  println("Character Already Played ~~>" + character.id)
                  finished(character.id) = character.owner
                } else {
                  // battle request here...
                  try {
                    val req: Seq[(String, String)] = Seq((character.id.toString, character.owner.toString), (availableCharacters.head._1._2.toString, availableCharacters.head._1._1.toString))

                    eosio.battleAction(req, UUID.randomUUID()).map {
                      // println(x)
                      // finished(character.id) = character.owner
                      case Left(x) => 
                        println("Battle Between Two Characters Has Failed:" + character.id + " ~~> " + availableCharacters.head._1._2)
                      // add to finished list after succesful battle ACTION..
                      case e => 
                        finished(character.id) = character.owner
                    }
                  } catch {
                    case e: Throwable => println("Battle Between Two Characters Has Failed:" + character.id + " ~~> " + availableCharacters.head._1._2)
                  }
                }
              }

              println("Available Characters To Play ~~> " +availableCharacters.size)
            }
          }
        }

        // Set Timeout to open and lock wallet for security
        // lock wallet after executing the battle command...
        // _ <- Future.successful(eosio.lockAllWallets())

        // update database ..
        _ <- Future.successful {
          val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
          self ! LoadGQUserTable(req)
        }
      } yield ()

    case RemoveCharacterWithNoLife =>
      // get all characters that has no life on DB
      characterRepo.getNoLifeCharacters.map { characters =>
        if (characters.size > 0)
          for {
            // unlock your wallet..
            _ <- Future.successful {
              eosio
                .unlockWalletAPI()
                .map(_ => Thread.sleep(2000))
            }
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
                      _ <- Future.successful {
                        if (playCount > 0) { // success
                          val history: GQCharacterDataHistory = GQCharacterDataHistory.fromCharacterData(ch, playCount)
                          // insert into game data history
                          // TODO: check the reason why it is failed..  
                          gQCDHistoryRepo.insert(history)
                        }
                        else None // return nothing
                      }
                    } yield ()
                }
                // add timeout for contract response..
                Thread.sleep(2000)
              }
              println("All Characters with no life has been removed")
            }
            _ <- Future.successful {
              eosio
                .lockAllWallets()
                .map(_ => Thread.sleep(2000))
            }
          } yield ()
        
        else println("Info: There's no eliminated character/s to remove in the DB.")
      }

    case LoadGQUserTable(request) =>
      eosio.getGQUsers(request).map(_.map(self ! _))

    case e => 
      println("Error: Unkown data received")
  }
}