package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.News

@Singleton
class NewsRepo @Inject()(
    dao: models.dao.NewsDAO,
    protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(news: News): Future[Int] =
    db.run(dao.Query += news)

  def +=(news: News): Future[Int] =
    db.run(dao.Query += news)

  def ++=(news: Seq[News]): Future[Seq[Int]] = {
    for {
      // check first if all data to be insert doesnt exist Future[Seq[Boolean]]
      hasExists <- Future.sequence(news.map(x => db.run(dao.Query.filter(_.title === x.title).exists.result)))
      checker <- Future.successful(hasExists.filter(_ == true))
      toInsert <- {
        if (checker.isEmpty) Future.sequence(news.map(v => add(v)))
        else Future(Seq.empty)
      }
    } yield (toInsert)
  }

  def --=(ids: Seq[UUID]): Future[Seq[Int]] = {
    for {
      // check first if all data to be insert doesnt exist Future[Seq[Boolean]]
      hasExists <- Future.sequence(ids.map(x => db.run(dao.Query(x).exists.result)))
      checker <- Future.successful(hasExists.filter(_ == false))
      toRemove <- {
        if (checker.isEmpty) Future.sequence(ids.map(v => -=(v)))
        else Future(Seq.empty)
      }
    } yield (toRemove)
  }

  def -=(id: UUID): Future[Int] =
    delete(id)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(news: News): Future[Int] =
    db.run(dao.Query.filter(_.id === news.id).update(news))

  def all(): Future[Seq[News]] =
    db.run(dao.Query.result)

  def exist(id: UUID): Future[Boolean] =
    db.run(dao.Query(id).exists.result)
}
