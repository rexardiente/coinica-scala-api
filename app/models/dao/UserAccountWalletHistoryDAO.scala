package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.wallet.support.{ UserAccountWalletHistory, CryptoJsonRpcHistory }

@Singleton
final class UserAccountWalletHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class UserAccountWalletHistoryTable(tag: Tag) extends Table[UserAccountWalletHistory](tag, "USER_ACCOUNT_WALLET_HISTORY") {
    def txHash = column[String] ("TRANSACTION_HASH", O.PrimaryKey)
    def id = column[UUID] ("ACCOUNT_ID")
    def currency = column[String] ("CURRENCY")
    def txType = column[String] ("TRANSACTION_TYPE")
    def data = column[CryptoJsonRpcHistory] ("DATA")
    def createdAt = column[Instant] ("CREATED_AT")
    def * = (txHash, id, currency, txType, data, createdAt) <> (UserAccountWalletHistory.tupled, UserAccountWalletHistory.unapply)
  }

  object Query extends TableQuery(new UserAccountWalletHistoryTable(_)) {
    def apply(hash: String) = this.withFilter(_.txHash === hash)
  }
}