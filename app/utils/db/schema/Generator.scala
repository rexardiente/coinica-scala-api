package utils.db.schema

import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.dao.user.UserDAO
import models.dao._

@Singleton
class Generator @Inject()(
    login : LoginDAO,
    user: UserDAO,
    game: GameDAO,
    genre: GenreDAO,
    task: TaskDAO,
    eosNetTx: EOSNetTransactionDAO,
    referral: ReferralDAO,
    ranking: RankingDAO,
    challenge: ChallengeDAO,
    challengeHistory: ChallengeHistoryDAO,
    challengeTracker: ChallengeTrackerDAO,
    gqCharacterData: GQCharacterDataDAO,
    gqCharacterGameHistory: GQCharacterGameHistoryDAO,
    gqCharacterDataHistory: GQCharacterDataHistoryDAO,
    // gqCharactersLifeTimeWinStreakDAO: GQCharactersLifeTimeWinStreakDAO,
    admin: AdminDAO,
    vipUser: VIPUserDAO,
    vipBenefit: VIPBenefitDAO,
    news: NewsDAO,
    overAllGameHistory: OverAllGameHistoryDAO,
    val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def createDDLScript() = {
    val schemas =
     login.Query.schema ++
      user.Query.schema ++
      game.Query.schema ++
      genre.Query.schema ++
      task.Query.schema ++
      eosNetTx.Query.schema ++
      referral.Query.schema ++
      ranking.Query.schema ++
      challenge.Query.schema ++
      challengeHistory.Query.schema ++
      challengeTracker.Query.schema ++
      gqCharacterData.Query.schema ++
      gqCharacterGameHistory.Query.schema ++
      gqCharacterDataHistory.Query.schema ++
      // gqCharactersLifeTimeWinStreakDAO.Query.schema ++
      admin.Query.schema ++
      vipUser.Query.schema ++
      vipBenefit.Query.schema ++
      news.Query.schema ++
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
