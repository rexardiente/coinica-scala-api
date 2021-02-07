package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ GQCharacterData, GQCharacterDataHistory, GQCharacterDataTrait }

@Singleton
class GQCharacterDataRepo @Inject()(
    dataDAO: models.dao.GQCharacterDataDAO,
    dataHistoryDAO: models.dao.GQCharacterDataHistoryDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

  def insert(data: GQCharacterData): Future[Int] =
    db.run(dataDAO.Query += data)

  def remove(user: String, id: String): Future[Int] =
    db.run(dataDAO.Query.filter(x => x.player === user && x.id === id).delete)

  def update(data: GQCharacterData): Future[Int] =
    db.run(dataDAO.Query.filter(x => x.player === data.owner && x.id === data.id).update(data))

  def all(): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.result)

  def exist(id: String): Future[Boolean] =
    db.run(dataDAO.Query(id).exists.result)

  def find(id: String): Future[Option[GQCharacterData]] =
    db.run(dataDAO.Query(id).result.headOption)

  def find(user: String, id: String): Future[Boolean] =
    db.run(dataDAO.Query.filter(x => x.player === user && x.id === id).exists.result)

  def getByUserAndID(user: String, id: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(x => x.player === user && x.id === id).result)

  def getByID(id: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.id === id).result)

  def getByUser(user: String): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.player === user).result)

  def getNoLifeCharacters(): Future[Seq[GQCharacterData]] =
    db.run(dataDAO.Query.filter(_.life < 1).result)

  // History Functions...
  def insertHistory(data: GQCharacterDataHistory): Future[Int] =
    db.run(dataHistoryDAO.Query += data)

  def getHistoryByUser(user: String): Future[Seq[GQCharacterDataHistory]] =
    db.run(dataHistoryDAO.Query.filter(_.player === user).result)

  def getCharacterHistoryByUserAndID(user: String, id: String): Future[Seq[GQCharacterDataHistory]] =
    db.run(dataHistoryDAO.Query.filter(v => v.player === user && v.id === id).result)

  // // Advanced table sorting..
  // def extremeN[T](n: Int, li: List[T]) (comp1: ((T, T) => Boolean), comp2: ((T, T) => Boolean)): List[T] = {
  //   def updateSofar (sofar: List[T], el: T) : List[T] =
  //     if (comp1 (el, sofar.head))
  //       (el :: sofar.tail).sortWith (comp2 (_, _))
  //     else sofar
  //     (li.take (n) .sortWith (comp2 (_, _)) foldLeft li.drop (n)) (updateSofar (_, _))
  // }
  // def top[T] (n: Int, li: List[T]) (implicit ord: Ordering[T]): Iterable[T] = {
  //   extremeN (n, li) (ord.lt (_, _), ord.gt (_, _))
  // }
  // def bottom[T] (n: Int, li: List[T]) (implicit ord: Ordering[T]): Iterable[T] = {
  //   extremeN (n, li) (ord.gt (_, _), ord.lt (_, _))
  // }

  import models.service.DynamicSortBySupport._
  import slick.ast.Ordering.Direction
  import slick.ast.Ordering
  def dynamicDataSort(): Future[Seq[GQCharacterDataTrait]] = {
    val sortsBy = Seq[(String, Direction)](("prize", Ordering.Desc))
    for {
      alive <- db.run(dataDAO.Query.dynamicSortBy(sortsBy).result)
      eliminated <- db.run(dataHistoryDAO.Query.dynamicSortBy(sortsBy).result)
    } yield (alive ++ eliminated)
  }
}
