package models.repo

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Ranking

@Singleton
class RankingRepo @Inject()(
    dao: models.dao.RankingDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def add(ranking: Ranking): Future[Int] =
    db.run(dao.Query += ranking)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(ranking: Ranking): Future[Int] =
    db.run(dao.Query.filter(_.id ===ranking.id).update(ranking))

  def all(limit: Int, offset: Int): Future[Seq[Ranking]] =
    db.run(dao.Query
      .drop(offset)
      .take(limit)
      .result)

  def exist(id: UUID): Future[Boolean] = 
    db.run(dao.Query(id).exists.result)

  def findByID(id: UUID, limit: Int, offset: Int): Future[Seq[Ranking]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .drop(offset)
      .take(limit)
      .result)
      
  def findByDaily( currentdate: Long, limit: Int, offset: Int): Future[Seq[Ranking]] = 
   db.run(dao.Query.filter(r =>  r.rankingcreated === currentdate) 
     .drop(offset)
     .take(limit)
     .result)
      
 
  def findByDateRange(startdate: Long, enddate : Long, limit: Int, offset: Int): Future[Seq[Ranking]] =
   db.run(dao.Query.filter(r => r.rankingcreated >= startdate && r.rankingcreated <= enddate ) 
     .drop(offset)
     .take(limit)
     .result)

  def findAll(limit: Int, offset: Int): Future[Seq[Ranking]] = 
    db.run(dao.Query.drop(offset).take(limit).result)

  def getSize(): Future[Int] =  
    db.run(dao.Query.length.result)

  
}