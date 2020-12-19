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
  private var isIntialized: Boolean = false
  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // load GhostQuest users to DB update for one time.. in case server is down..
    val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    self ! VerifyGQUserTable(req)

    
    // scheduled on every 3 minutes
    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 3.minute, interval = 3.minute)(() => self ! BattleScheduler)

    // scheduled on every 1 hr to verify data integrity..
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => {
    //   println("verify data integrity")
    //   self ! VerifyGQUserTable(req)})
  }

  def receive: Receive = {
    case connect: Connect => 
      // Indicators if there's new Client Connected..
      // println(connect)

    case GQRowsResponse(rows, hasNext, nextKey) =>
      val seqCharacters = ListBuffer[GQCharacterData]()
      val characterPrevMatches = ListBuffer[((String, String), Seq[GQCharacterPrevMatch])]()
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
                characterPrevMatches.append(((username, ch.key), ch.value.match_history))
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
                      println("\t"+ info.owner + " ~> " + info.id + " UPDATED")
                    else
                      println("\t"+ info.owner + " ~> " + info.id + " FAILED TO UPDATE")
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
          if (!isIntialized) {
            // change isIntialized after first load..
            print("\n============\tInitializing History DB  ============") 
            if (hasNext == false)
              isIntialized = true

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

            Future.sequence(setOfCharactersGameHistory.map(history => {
              gQGameHistoryRepo
                .exist(history.game_id, history.player)
                .map { isExists =>
                  if (!isExists) 
                    gQGameHistoryRepo.insert(history).map(_ => true)
                  else 
                    Future(false)
                    // println(history.game_id + " history already exists.")
                  // else {
                  //   gQGameHistoryRepo.update(history).map { update =>
                  //     if (update > 0) 
                  //       println("STATUS UPDATE SUCCESS: " + info.owner.toUpperCase + " ~> character " + info.key)
                  //     else
                  //       println("STATUS UPDATE FAILED: " + info.owner.toUpperCase + " ~> character " + info.key)
                  //   }
                  // }
                }
                .flatten
            })).map { x => 
              if(x.forall(_ == true)) 
                println("\n============\tHistory has been updated ============\n") 
              else
                println("\n============\tHistory is up to date    ============\n") 
            }
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

      _ <- characterRepo
            .all()
            .map { characters =>
              // shuffled chracters list
              val shuffled: Seq[GQCharacterData] = scala.util.Random.shuffle(characters).filter(_.character_life > 0)
              
              // convert Seq[chracters] to HashMap for Done playing or no available to play...
              val finished = new HashMap[String, String]() // chracter ID

              // loop all chracters to find available enemy and do the battle...
              shuffled.foreach { character =>
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
                          println(x)
                          println("Error: Battle Between Two Characters:" + character.id + " ~~> " + availableCharacters.head._1._2)
                        // add to finished list after succesful battle ACTION..
                        case e => 
                          finished(character.id) = character.owner
                      }
                    } catch {
                      case e: Throwable => println("Error: Battle Between Two Characters:" + character.id + " ~~> " + availableCharacters.head._1._2)
                    }
                  }
                }

                println("Available Characters To Play ~~> " +availableCharacters.size)
                defaultThreadSleep()
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
               defaultThreadSleep()
              }
              println("All Characters with no life has been removed")
            }
            _ <- Future.successful(support.lockAllWallets())
          } yield ()
        
        else println("Info: There's no eliminated character/s to remove in the DB.")
      }

    case VerifyGQUserTable(request) =>
      eosio.getGQUsers(request).map(_.map(self ! _))

    case e => 
      println("Error: Unkown data received")
  }

  def defaultThreadSleep(): Unit = Thread.sleep(2000)
}