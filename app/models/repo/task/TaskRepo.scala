package models.repo.task

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.task.Task

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class TaskRepo @Inject()(
    dao: models.dao.task.TaskDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {

   case class PaginatedResult[T](
  totalCount: Int, 
  entities: List[T], 
  hasNextPage: Boolean
)
  import profile.api._

  def add(task: Task): Future[Int] =
    db.run(dao.Query += task)

  def delete(id: UUID): Future[Int] =
    db.run(dao.Query(id).delete)

  def update(task: Task): Future[Int] =
    db.run(dao.Query.filter(_.id ===task.id).update(task))

  def all(limit: Int, offset: Int): Future[Seq[Task]] =
    db.run(dao.Query
        .drop(offset)
      .take(limit)
       .result)
    

  def exist(id: UUID): Future[Boolean] = db.run(dao.Query(id).exists.result)


  def findByID(id: UUID, limit: Int, offset: Int): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.id === id  )
      .drop(offset)
      .take(limit)
      .result
      .headOption)


  def findByDaily(id: UUID, currentdate: Instant): Future[Seq[Task]] = 
   db.run(dao.Query.filter(r => r.id === id && r.datecreated === currentdate) 
      .result)
  

  def findByWeekly(id: UUID, startdate: Instant, enddate : Instant): Future[Seq[Task]] =
   db.run(dao.Query.filter(r => r.id === id && r.datecreated >= startdate && r.datecreated <= enddate ) 
      .result)
      /*
def query = TableQuery[Task]
 def findAll( limit: Int, offset: Int) = db.run {
  for {
    comments <- query
                     .drop(offset).take(limit)
                     .result
    numberOfComments <- query.length.result
  } yield PaginatedResult(
    totalCount = numberOfComments,
    entities = comments.toList,
    hasNextPage = numberOfComments - (offset + limit) > 0
  )
}
*/
def findAll(limit: Int, offset: Int): Future[Seq[Task]] =  {
    db.run(dao.Query.drop(offset).take(limit).result)
  }
  def getSize(): Future[Int] =  {
    db.run(dao.Query.length.result)
  }

  def paginatedRes(limit: Int, offset: Int): Future[PaginatedResult[Task]] = {
    val futSeqTx: Future[Seq[Task]] = findAll(limit, offset)
    (futSeqTx.flatMap { seqTx =>
      getSize.map { size =>
        PaginatedResult(size, seqTx.toList, size - (offset + limit) > 0)
      }
    })
  }


 

  def findBygameName(gameID: UUID): Future[Option[Task]] =
    db.run(dao.Query.filter(r => r.gameID === gameID)
      .result
      .headOption)

  }
