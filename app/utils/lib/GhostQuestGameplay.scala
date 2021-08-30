package utils.lib

import java.util.UUID
import scala.util.Random
import scala.collection.mutable.ListBuffer
import models.domain.eosio._

@javax.inject.Singleton
class GhostQuestGameplay[T <: GhostQuestCharacter](plyr1: T, plyr2: T) {
	// Initialize game default values..
	private val gameID 			 : UUID = java.util.UUID.randomUUID
	private val characters 	 : Map[String, T] =
		if (plyr1.value.speed > plyr2.value.speed) Map(plyr1.key -> plyr1, plyr2.key -> plyr2)
		else Map(plyr2.key -> plyr2, plyr1.key -> plyr1)
	private val logs  			 : ListBuffer[GhostQuestCharacterGameLog] = ListBuffer.empty[GhostQuestCharacterGameLog]
	private var battleResult : Option[GhostQuestBattleResult] = None
	private var gameRounds   : Int = 1
	private var plyr1TotalDmg: Int = 0
	private var plyr2TotalDmg: Int = 0

	private def run(): Unit = {
		if (plyr1.value.character_life == 0 || plyr2.value.character_life == 0) None
		else
			while (plyr1TotalDmg < characters(plyr1.key).value.hitpoints && plyr2TotalDmg < characters(plyr2.key).value.hitpoints) {
				// perform damage calculation
				val partialDmg2: (GhostQuestCharacterGameLog, Int) = dmgOutput(characters.head._2, characters.last._2)
				logs += partialDmg2._1
				plyr2TotalDmg += partialDmg2._2
				gameRounds += 1

				if (characters.last._2.value.hitpoints > plyr2TotalDmg) {
					val partialDmg1: (GhostQuestCharacterGameLog, Int) = dmgOutput(characters.last._2, characters.head._2)
					logs += partialDmg1._1
					plyr1TotalDmg += partialDmg1._2
					gameRounds += 1

					if (characters.head._2.value.hitpoints < plyr1TotalDmg)
						battleResult = Some(GhostQuestBattleResult(
																gameID,
																Map(characters.last._2.key -> (characters.last._2.value.owner_id, true),
																		characters.head._2.key -> (characters.head._2.value.owner_id, false)).toList,
																logs.toList))
				}
				else battleResult = Some(GhostQuestBattleResult(
																gameID,
																Map(characters.head._2.key -> (characters.head._2.value.owner_id, true),
																		characters.last._2.key -> (characters.last._2.value.owner_id, false)).toList,
																	logs.toList))
			}
	}

	private def dmgOutput(attacker: T, defender: T): (GhostQuestCharacterGameLog, Int) = {
		val chance: Int =
			if (attacker.value.luck > attacker.value.speed &&
				attacker.value.luck > attacker.value.defense &&
				attacker.value.luck > attacker.value.attack) { attacker.value.luck / 3 }
			else { attacker.value.luck / 4 }
		val luck: Int = new Random().nextInt(100)
		val dmgDWT: Int = new Random().nextInt(attacker.value.attack / 16 + 1)
		val dmgRed: Int = (attacker.value.attack * defender.value.defense) / 100

		// determine critical and damage width for final damage dealt
		val overAllDmg: (Int, Boolean) =
			if (luck <= chance)
		      (if (luck % 2 == 0) attacker.value.attack + dmgDWT else attacker.value.attack - dmgDWT, true)
		    else
		    	(if (luck % 2 == 0) attacker.value.attack - dmgRed + dmgDWT else attacker.value.attack - dmgRed - dmgDWT, false)

	  // componse battle log and overall damage taken..
	  (new GhostQuestCharacterGameLog(gameRounds, attacker.value.owner_id, defender.value.owner_id, overAllDmg._1, overAllDmg._2), overAllDmg._1)
	}
	// private def result(loser: T, winner: T): GhostQuestBattleResult = {
	// 	var winnerCharacter: GhostQuestCharacter = winner.copy(status = 4, life = (winner.value.character_life + 1))
	// 	val loserCharacter: GhostQuestCharacter =
	// 		if (loser.value.character_life == 1)
	// 			loser.copy(status = 6, life = 0)
	// 		else
	// 			loser.copy(status = 5, life = (loser.value.character_life - 1))
	// 	GhostQuestBattleResult(gameID, Map(winner.key -> true, loser.key -> false), logs.toList)
	// }
	def result(): Option[GhostQuestBattleResult] = battleResult
	// start battle..
	try { run() } catch { case _: Throwable =>	battleResult = None }
}