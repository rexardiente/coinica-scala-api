package utils.lib

import java.util.UUID
import scala.util.Random
import scala.collection.mutable.ListBuffer
import models.domain.eosio.{ GameLog, GQBattleResult }
import models.domain.eosio.GQ.v2.GQCharacterData

@javax.inject.Singleton
class GQBattleCalculation[T <: GQCharacterData](plyr1: T, plyr2: T) {
	// Initialize game default values..
	private val gameID 			 : UUID 									= java.util.UUID.randomUUID
	private val characters 	 : Map[String, T] 				=
		if (plyr1.speed > plyr2.speed) Map(plyr1.key -> plyr1, plyr2.key -> plyr2)
		else Map(plyr2.key -> plyr2, plyr1.key -> plyr1)
	private val logs  			 : ListBuffer[GameLog] 		= ListBuffer.empty[GameLog]
	private var battleResult : Option[GQBattleResult] = None
	private var gameRounds   : Int = 1
	private var plyr1TotalDmg: Int = 0
	private var plyr2TotalDmg: Int = 0

	private def run(): Unit = {
		if (plyr1.life == 0 || plyr2.life == 0) None
		else
			while (plyr1TotalDmg < characters(plyr1.key).hp && plyr2TotalDmg < characters(plyr2.key).hp) {
				// perform damage calculation
				val partialDmg2: (GameLog, Int) = dmgOutput(characters.head._2, characters.last._2)
				logs += partialDmg2._1
				plyr2TotalDmg += partialDmg2._2
				gameRounds += 1

				if (characters.last._2.hp > plyr2TotalDmg) {
					val partialDmg1: (GameLog, Int) = dmgOutput(characters.last._2, characters.head._2)
					logs += partialDmg1._1
					plyr1TotalDmg += partialDmg1._2
					gameRounds += 1

					if (characters.head._2.hp < plyr1TotalDmg)
						battleResult = Some(GQBattleResult(
																gameID,
																Map(characters.last._2.key -> (characters.last._2.owner, true),
																		characters.head._2.key -> (characters.head._2.owner, false)),
																logs.toList))
				}
				else battleResult = Some(GQBattleResult(
																gameID,
																Map(characters.head._2.key -> (characters.head._2.owner, true),
																		characters.last._2.key -> (characters.last._2.owner, false)),
																	logs.toList))
			}
	}

	private def dmgOutput(attacker: T, defender: T): (GameLog, Int) = {
		val chance: Int = if (attacker.`class` == 4) { attacker.luck / 3 } else { attacker.luck / 4 }
		val luck: Int = new Random().nextInt(100)
		val dmgDWT: Int = new Random().nextInt(attacker.attack / 16 + 1)
		val dmgRed: Int = (attacker.attack * defender.defense) / 100

		// determine critical and damage width for final damage dealt
		val overAllDmg: (Int, Boolean) =
			if (luck <= chance)
		      (if (luck % 2 == 0) attacker.attack + dmgDWT else attacker.attack - dmgDWT, true)
		    else
		    	(if (luck % 2 == 0) attacker.attack - dmgRed + dmgDWT else attacker.attack - dmgRed - dmgDWT, false)

	  // componse battle log and overall damage taken..
	  (new GameLog(gameRounds, attacker.owner, defender.owner, overAllDmg._1, overAllDmg._2), overAllDmg._1)
	}
	// private def result(loser: T, winner: T): GQBattleResult = {
	// 	var winnerCharacter: GQCharacterData = winner.copy(status = 4, life = (winner.life + 1))
	// 	val loserCharacter: GQCharacterData =
	// 		if (loser.life == 1)
	// 			loser.copy(status = 6, life = 0)
	// 		else
	// 			loser.copy(status = 5, life = (loser.life - 1))
	// 	GQBattleResult(gameID, Map(winner.key -> true, loser.key -> false), logs.toList)
	// }
	def result(): Option[GQBattleResult] = battleResult
	// start battle..
	try { run() } catch { case _: Throwable =>	battleResult = None }
}