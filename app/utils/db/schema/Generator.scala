package utils.db.schema

import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.dao._

@Singleton
class Generator @Inject()(
    platform: PlatformConfigDAO,
    user: UserAccountDAO,
    userWalletHistory: UserAccountWalletHistoryDAO,
    genre: GenreDAO,
    task: TaskDAO,
    dailyTask: DailyTaskDAO,
    taskHistory: TaskHistoryDAO,
    eosNetTx: EOSNetTransactionDAO,
    referral: ReferralHistoryDAO,
    ranking: RankingHistoryDAO,
    challenge: ChallengeDAO,
    challengeHistory: ChallengeHistoryDAO,
    challengeTracker: ChallengeTrackerDAO,
    ghostQuestCharacterDAO: GhostQuestCharacterDAO,
    ghostQuestCharacterHistoryDAO: GhostQuestCharacterHistoryDAO,
    ghostQuestCharacterGameHistoryDAO: GhostQuestCharacterGameHistoryDAO,
    ghostQuestBattleResultDAO: GhostQuestBattleResultDAO,
    mahjongHiloHistoryDAO: MahjongHiloHistoryDAO,
    vipBenefit: VIPBenefitDAO,
    overAllGameHistory: OverAllGameHistoryDAO,
    val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def createDDLScript() = {
    val schemas =
      platform.Query.schema ++
      user.UserAccountQuery.schema ++
      userWalletHistory.Query.schema ++
      user.VIPUserQuery.schema ++
      user.UserWalletQuery.schema ++
      user.FailedCoinDepositQuery.schema ++
      genre.Query.schema ++
      task.Query.schema ++
      dailyTask.Query.schema ++
      taskHistory.Query.schema ++
      eosNetTx.Query.schema ++
      referral.Query.schema ++
      ranking.Query.schema ++
      challenge.Query.schema ++
      challengeHistory.Query.schema ++
      challengeTracker.Query.schema ++
      ghostQuestCharacterDAO.Query.schema ++
      ghostQuestCharacterHistoryDAO.Query.schema ++
      ghostQuestCharacterGameHistoryDAO.Query.schema ++
      ghostQuestBattleResultDAO.Query.schema ++
      mahjongHiloHistoryDAO.Query.schema ++
      vipBenefit.Query.schema ++
      overAllGameHistory.Query.schema


    val writer = new java.io.PrintWriter("target/schema.sql")
    writer.write("# --- !Ups\n\n")
    schemas.createStatements.foreach { s => writer.write(s + ";\n\n") }

    writer.write("\n\n# --- !Downs\n\n")
    schemas.dropStatements.foreach { s => writer.write(s + ";\n") }

    println("Schema definitions are written")

    writer.close()
  }

  createDDLScript()
}
