// package models.repo.eosio

// import javax.inject.{ Inject, Singleton }
// import java.time.Instant
// import java.util.UUID
// import java.time.Instant
// import scala.collection.mutable.HashMap
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.Future
// import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
// // import Ordering.Double.IeeeOrdering
// import models.service.DynamicSortBySupport._
// import models.domain.eosio._
// import models.domain.eosio.GQ.v2.{ GQCharacterData, GQCharacterDataHistory }

// @Singleton
// class GQCharacterDataRepo @Inject()(
//     dataDAO: models.dao.GQCharacterDataDAO,
//     dataHistoryDAO: models.dao.GQCharacterDataHistoryDAO,
//     gameHistoryDAO: models.dao.GQCharacterGameHistoryDAO,
//     protected val dbConfigProvider: DatabaseConfigProvider
//   ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
//   import profile.api._

//   def mergeSeq[A, T <: Seq[A]](seq1: T, seq2: T) = (seq1 ++ seq2)

//   def insert(data: GQCharacterData): Future[Int] =
//     db.run(dataDAO.Query += data)

//   def remove(user: UUID, key: String): Future[Int] =
//     db.run(dataDAO.Query.filter(x => x.owner === user && x.key === key).delete)

//   def update(data: GQCharacterData): Future[Int] =
//     db.run(dataDAO.Query.filter(x => x.owner === data.owner && x.key === data.key).update(data))

//   def all(): Future[Seq[GQCharacterData]] =
//     db.run(dataDAO.Query.result)

//   def exist(key: String): Future[Boolean] =
//     db.run(dataDAO.Query(key).exists.result)

//   def find(id: String): Future[Option[GQCharacterData]] =
//     db.run(dataDAO.Query(id).result.headOption)

//   def find(user: UUID, id: String): Future[Boolean] =
//     db.run(dataDAO.Query.filter(x => x.owner === user && x.key === id).exists.result)

//   def getByUserAndID(user: UUID, id: String): Future[Seq[GQCharacterData]] =
//     db.run(dataDAO.Query.filter(x => x.owner === user && x.key === id).result)

//   def getByID(id: String): Future[Option[GQCharacterData]] =
//     db.run(dataDAO.Query.filter(_.key === id).result.headOption)

//   def getByUser(user: UUID): Future[Seq[GQCharacterData]] =
//     db.run(dataDAO.Query.filter(_.owner === user).result)

//   def getNoLifeCharacters(): Future[Seq[GQCharacterData]] =
//     db.run(dataDAO.Query.filter(_.life < 1).result)

//   def updateOrInsertAsSeq(data: GQCharacterData): Future[Int] = {
//     for {
//         isExists <- exist(data.key)
//         result <- if (isExists) update(data) else insert(data)
//       } yield (result)
//   }
//   // Character Data History
//   def insertDataHistory(data: GQCharacterDataHistory): Future[Int] =
//     db.run(dataHistoryDAO.Query += data)

//   def getHistoryByUser(user: UUID): Future[Seq[GQCharacterDataHistory]] =
//     db.run(dataHistoryDAO.Query.filter(_.owner === user).result)

//   def getCharacterHistoryByUserAndID(user: UUID, id: String): Future[Seq[GQCharacterDataHistory]] =
//     db.run(dataHistoryDAO.Query.filter(v => v.owner === user && v.key === id).result)

//   def getCharacterHistoryByID(id: String): Future[Option[GQCharacterDataHistory]] =
//     db.run(dataHistoryDAO.Query.filter(_.key === id).result.headOption)

//   // // Advanced table sorting..
//   // def extremeN[T](n: Int, li: List[T]) (comp1: ((T, T) => Boolean), comp2: ((T, T) => Boolean)): List[T] = {
//   //   def updateSofar (sofar: List[T], el: T) : List[T] =
//   //     if (comp1 (el, sofar.head))
//   //       (el :: sofar.tail).sortWith (comp2 (_, _))
//   //     else sofar
//   //     (li.take (n) .sortWith (comp2 (_, _)) foldLeft li.drop (n)) (updateSofar (_, _))
//   // }
//   // def top[T] (n: Int, li: List[T]) (implicit ord: Ordering[T]): Iterable[T] = {
//   //   extremeN (n, li) (ord.lt (_, _), ord.gt (_, _))
//   // }
//   // def bottom[T] (n: Int, li: List[T]) (implicit ord: Ordering[T]): Iterable[T] = {
//   //   extremeN (n, li) (ord.gt (_, _), ord.lt (_, _))
//   // }

//   // import slick.ast.Ordering.Direction
//   // import slick.ast.Ordering
//   // sort by charcters
//   // def dynamicDataSort(query: String, limit: Int): Future[Seq[GQCharacterDataTrait]] = {
//   //   val sortsBy = Seq[(String, Direction)]((query, Ordering.Desc))
//   //   (for {
//   //       alive <- db.run(dataDAO.Query.dynamicSortBy(sortsBy).result)
//   //       eliminated <- db.run(dataHistoryDAO.Query.dynamicSortBy(sortsBy).result)
//   //   } yield (alive ++ eliminated))
//   // }
// }
