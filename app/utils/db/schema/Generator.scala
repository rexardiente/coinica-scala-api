package utils.db.schema

import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.dao.user.UserDAO
import models.dao.{ GameDAO, GenreDAO, TaskDAO, TransactionDAO, ReferralDAO }

@Singleton
class Generator @Inject()(
    userDAO: UserDAO,
    gameDAO: GameDAO,
    genreDAO: GenreDAO,
    taskDAO: TaskDAO,
    txDAO: TransactionDAO,
    referralDAO: ReferralDAO,
    val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def createDDLScript() = {
    val schemas = 
      userDAO.Query.schema ++
      gameDAO.Query.schema ++
      genreDAO.Query.schema ++
      taskDAO.Query.schema ++
      txDAO.Query.schema ++
      referralDAO.Query.schema 
      

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
