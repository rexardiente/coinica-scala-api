// package models.dao

// import javax.inject.{ Inject, Singleton }
// import java.util.UUID
// import java.time.Instant
// import play.api.libs.json._
// import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
// import models.domain.RankingHistory

// @Singleton
// final class RankingHistoryDAO @Inject()(
//     protected val dbConfigProvider: DatabaseConfigProvider,
//   ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
//   import profile.api._


//   protected class RankingHistoryTable(tag: Tag) extends Table[RankingHistory](tag, "RANKING_HISTORY") {
//     def id = column[UUID] ("ID", O.PrimaryKey)
//     def name = column[String] ("NAME")
//     def bets = column[Double] ("BETS")
//     def profit = column[Double] ("PROFIT")
//     def multiplieramount = column[Double] ("MULTIPLIERAMOUNT")
//     def rankingcreated = column[Long] ("RANKINGCREATED")

//     def * = (id, name, bets, profit, multiplieramount, rankingcreated) <> ((Ranking.apply _).tupled, Ranking.unapply)
//   }

//   object Query extends TableQuery(new RankingTable(_)) {
//     def apply(id: UUID) = this.withFilter(_.id === id)
//   }

// }
