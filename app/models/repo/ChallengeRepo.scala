package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Challenge

@Singleton
class ChallengeRepo @Inject()(
    dao: models.dao.ChallengeDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  private val createAtFn = SimpleFunction.unary[Instant, Long]("CREATED_AT")

  def add(challenge: Challenge): Future[Int] =
    db.run(dao.Query += challenge)
  // def ++=(challenges: Seq[Challenge]): Future[Seq[Int]] = {
  //   for {
  //     hasExists <- Future.sequence(challenges.map(x => db.run(dao.Query.filter(_.gameID === x.gameID).exists.result)))
  //     checker <- Future.successful(hasExists.filter(_ == true))
  //     toInsert <- {
  //       if (checker.isEmpty) Future.sequence(challenges.map(v => add(v)))
  //       else Future(Seq.empty)
  //     }
  //   } yield (toInsert)
  // }
  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(challenge: Challenge): Future[Int] =
    db.run(dao.Query.filter(_.id ===challenge.id).update(challenge))

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)
  def findAll(limit: Int, offset: Int): Future[Seq[Challenge]] =
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =
    db.run(dao.Query.length.result)
}