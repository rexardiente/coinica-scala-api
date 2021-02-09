package models.repo.eosio

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.util.UUID
import java.time.Instant
import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import Ordering.Double.IeeeOrdering
import models.service.DynamicSortBySupport._
import models.domain.eosio._

@Singleton
class GQCharacterDataRepo @Inject()(
    dataDAO: models.dao.GQCharacterDataDAO,
    dataHistoryDAO: models.dao.GQCharacterDataHistoryDAO,
    gameHistoryDAO: models.dao.GQCharacterGameHistoryDAO,
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

  import slick.ast.Ordering.Direction
  import slick.ast.Ordering

  // sort by charcters
  def dynamicDataSort(query: String, limit: Int): Future[Seq[GQCharacterDataTrait]] = {
    val sortsBy = Seq[(String, Direction)]((query, Ordering.Desc))
    (for {
        alive <- db.run(dataDAO.Query.dynamicSortBy(sortsBy).result)
        eliminated <- db.run(dataHistoryDAO.Query.dynamicSortBy(sortsBy).result)
    } yield (alive ++ eliminated))
  }

  def highestPerWeekOrDay(range: Long, limit: Int): Future[Seq[(String, (String, Double))]] = {
    // { if (query == "week") ((24*60*60) * 7) else (24*60*60) }
    val characters = HashMap.empty[String, (String, Double)]
    val coverage   = Instant.now().getEpochSecond - range

    // get all charcters involved in 1 week transactions
    for {
      txs <- db.run(gameHistoryDAO.Query.filter(_.timeExecuted >= coverage).result)
      calculate <- Future.successful {
        txs.map { tx =>
          // processTxStatus
          tx.status.foreach({ stat =>
            // check if it exists on HashMap
            if (!characters.exists(_._1 == stat.char_id)) characters(stat.char_id) = (stat.player, 0)

            val (player, amount) = characters(stat.char_id)
            // update characters balances on HashMap
            if (stat.isWin) characters.update(stat.char_id, (player, amount + 1))
            else characters.update(stat.char_id, (player, amount - 1))
          })
        }
      }
    } yield (characters.toSeq.sortBy(- _._2._2).take(limit))
  }
}
