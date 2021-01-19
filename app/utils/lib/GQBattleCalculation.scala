package utils.lib

import java.util.UUID
import scala.util.Random
import scala.collection.mutable.ListBuffer
import models.domain.eosio.{ GQCharacterData, GameLog, GQBattleResult }

class GQBattleCalculation[T <: GQCharacterData](plyr1: T, plyr2: T) {

	// track damages on each characters..
	// private case class Characters[A](var value: A) { def apply() = value }
	// private def addUpdateStatus[T](n: String, t: T, k: Characters[Map[String,T]]) = { k.value += (n -> t); t }

	val characters: Map[String, T] = Map(plyr1.id -> plyr1, plyr2.id -> plyr2)
	val logs: ListBuffer[GameLog] = ListBuffer.empty[GameLog]
	private var battleResult: Option[GQBattleResult] = None

	// Initialize game..
	var rounds: Int = 0
	var dmg1: Int = 0
	var dmg2: Int = 0

	private def run(): Unit = {
		// if (plyr1.character_life == 0 || plyr2.character_life == 0) return
		while (dmg1 < characters(plyr1.id).initial_hp && dmg2 < characters(plyr2.id).initial_hp) {
			// determine which monster attack first
			if (plyr1.speed > plyr2.speed) {
				// perform damage calculation
				val partialDmg2: (GameLog, Int) = dmgOutput(characters(plyr1.id), characters(plyr2.id))
				logs += partialDmg2._1
				dmg2 += partialDmg2._2
				rounds += 1

				if (characters(plyr2.id).initial_hp > dmg2) {
					val partialDmg1: (GameLog, Int) = dmgOutput(characters(plyr2.id), characters(plyr1.id))
					logs += partialDmg1._1
					dmg1 += partialDmg1._2
					rounds += 1

					if (characters(plyr1.id).initial_hp < dmg1)
						battleResult = Some(result(characters(plyr1.id), characters(plyr2.id)))
				}
				else battleResult = Some(result(characters(plyr2.id), characters(plyr1.id)))
      }

      else
			{
				val partialDmg1: (GameLog, Int) = dmgOutput(characters(plyr2.id), characters(plyr1.id))
				logs += partialDmg1._1
				dmg1 += partialDmg1._2
				rounds += 1

			  if (characters(plyr1.id).initial_hp > dmg1) {
			  	val partialDmg2: (GameLog, Int) = dmgOutput(characters(plyr1.id), characters(plyr2.id))
					logs += partialDmg2._1
					dmg2 += partialDmg2._2
					rounds += 1

					if (characters(plyr2.id).initial_hp < dmg2)
						battleResult = Some(result(characters(plyr2.id), characters(plyr1.id)))
			  }
			  else battleResult = Some(result(characters(plyr1.id), characters(plyr2.id)))
			}
		}
	}

	private def dmgOutput(attacker: T, defender: T): (GameLog, Int) = {
		val chance: Int = if (attacker.ghost_class == 4) { attacker.luck / 3 } else { attacker.luck / 4 }
		val luck: Int = new Random().nextInt(100)
		val dmgDWT: Int = new Random().nextInt(attacker.attack / 16 + 1)
		val dmgRed: Int = (attacker.attack * defender.defense) / 100

		// determine critical and damage width for final damage dealt
		val overAllDmg: Int =
			if (luck <= chance)
	      if (luck % 2 == 0) attacker.attack + dmgDWT else attacker.attack - dmgDWT
	    else
	    	if (luck % 2 == 0) attacker.attack - dmgRed + dmgDWT else attacker.attack - dmgRed - dmgDWT

	  // componse battle log and overall damage taken..
	  (new GameLog(rounds, attacker.owner, defender.owner, overAllDmg), overAllDmg)
	  // ("Round " + rounds + " : Character of " + defender.owner + " took " + overAllDmg + " damage from character of " + attacker.owner + ".", overAllDmg)
	}

	private def result(loser: T, winner: T): GQBattleResult = {
		val loserCharacter: GQCharacterData =
		if (loser.character_life == 1)
			loser.copy(status = 6, character_life = 0)
		else
			loser.copy(status = 5, character_life = (loser.character_life - 1))

		var winnerCharacter: GQCharacterData = winner.copy(status = 4, character_life = (winner.character_life + 1))
		// logs += "Battle Outcome : Character of " + winnerCharacter.owner + " won against character of" + loserCharacter.owner

		GQBattleResult(
			UUID.randomUUID,
			Map(winnerCharacter.id -> true, loserCharacter.id -> false),
			logs.toList)
	}

	def getBattleResult(): Option[GQBattleResult] = battleResult

	// start battle..
	run()
}

// game_id, status[player, status], logs, time