package akka

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.Logger
import models.domain.eosio._
import models.repo.eosio._
import akka.domain.common.objects._
import utils.lib.EOSIOSupport

object SchedulerActor {
  def props = Props[SchedulerActor]
}

@Singleton
class SchedulerActor @Inject()(
      ws: WSClient, 
      characterRepo: GQCharacterDataRepo, 
      gqGameHistoryRepo: GQCharacterGameHistoryRepo)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) extends Actor {
  val logger       : Logger      = Logger(this.getClass())
  val eosioSupport :EOSIOSupport = new EOSIOSupport(ws)

  override def preStart: Unit = {
    super.preStart
    println("SchedulerActor Initialized!!!")

    // load GhostQuest users to DB update for one time.. in case server is down..
    // val req: TableRowsRequest = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    // self ! LoadGQUserTable(req)

    self ! BattleScheduler

    // scheduled on every 3 minutes
    // actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 1.minute)(() => self ! BattleScheduler)
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
                  UUID.randomUUID(),
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
              characterRepo.find(info.owner, info.chracterID).map { isExists =>
                if (!isExists) 
                  characterRepo.insert(info)
                else {
                  characterRepo.update(info).map { update =>
                    if (update > 0) 
                      println("Update Successful: " + info.owner.toUpperCase + " ~> character " + info.chracterID)
                    else
                      println("Update Failed: " + info.owner.toUpperCase + " ~> character " + info.chracterID)
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
            gqGameHistoryRepo.exist(history.game_id, history.player).map { isExists =>
              if (!isExists) gqGameHistoryRepo.insert(history)
              // else {
              //   gqGameHistoryRepo.update(history).map { update =>
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
      // else 
      //   ???

    case BattleScheduler => 
      // import java.util.Random
      // Note: no more chracters that are eliminated in the game..
      characterRepo.all().map { characters =>
        // val rand = new scala.util.Random()
        // val shuffled: Seq[GQCharacterData] = rand.shuffle(characters)

        // val map = new scala.collection.mutable.HashMap[String, GQCharacterData]() // chracter ID
        // shuffled.foreach(each => map(each.chracterID) = each)
        // println(map.size)

        println(res)


        // shuffled.map { x =>
        //   println(x.chracterID + "\t" + x.owner)
        // }
        // shuffled.grouped(2)
        // println(shuffled(0))
        // println(characters(0))
        // characters.filter(_ => rand.nextInt(100) < 50)
        // println(Json.toJson(characters))
      }



      // logger.info("Executing SchedulerActor in every 3 min...")
      // for {
      //   chracters <- characterRepo.all() // Future[Seq[GQCharacterData]]
      //   grouped <- Future.successful(chracters.grouped(2).toList)
      // } yield grouped.map { list =>
      //   println(list)
      // }

      // n = List of characters and save to DB
      // battle Action response >>
      // if (life > 0)
      // update chracter info and gamehistory...
      // else
      // updata chracter status to eliminated..

    case LoadGQUserTable(request) =>
      eosioSupport.getGQUsers(request).map(_.map(self ! _))

    case "GQ_user_tbl_update" => println("GQ_user_tbl_update")
    case e => 
      println("unkown data received!!!")
  }
}