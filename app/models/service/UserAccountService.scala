package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain.{ PaginatedResult, UserAccount, VIPUser, UserAccountWallet }
import models.domain.wallet.support._
import models.repo.{
  UserAccountRepo,
  VIPUserRepo,
  UserAccountWalletRepo,
  UserAccountWalletHistoryRepo,
  FailedCoinDepositRepo
}
import utils.lib.MultiCurrencyHTTPSupport
import utils.SystemConfig.DEFAULT_WEI_VALUE

@Singleton
class UserAccountService @Inject()(
      userAccountRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      userWalletRepo: UserAccountWalletRepo,
      userWalletHistoryRepo: UserAccountWalletHistoryRepo,
      failedCoinDepositRepo: FailedCoinDepositRepo,
      httpSupport: MultiCurrencyHTTPSupport) {
  def paginatedResult[T](result: Seq[T], size: Int, limit: Int, offset: Int) = Future.successful {
    val currentPage: Int = limit * offset
    val hasNext = ((size - currentPage) - limit) > 0
    PaginatedResult[T](result.size, result.toList, hasNext)
  }
  def getAccountByID(id: UUID): Future[Option[UserAccount]] =
  	userAccountRepo.getByID(id)
  def getAccountByGameID(id: Int): Future[Option[UserAccount]] =
    userAccountRepo.getByGameID(id)
  def getAccountByName(name: String): Future[Option[UserAccount]] =
  	userAccountRepo.getByName(name)
  def updateUserAccount(acc: UserAccount): Future[Int] =
  	userAccountRepo.update(acc)
  def getAccountByCode(code: String): Future[Option[UserAccount]] =
  	userAccountRepo.getAccountByReferralCode(code)
  def getAccountByEmailAddress(email: String): Future[Option[UserAccount]] =
    userAccountRepo.getAccountByEmailAddress(email)
  def getAccountByUserNamePassword(user: String, pass: String): Future[Option[UserAccount]] =
  	userAccountRepo.getAccountByUserNamePassword(user, pass)
  def newUserAcc(acc: UserAccount): Future[Int] =
  	userAccountRepo.add(acc)
  def exists(username: String, password: String): Future[Boolean] =
    userAccountRepo.exist(username, password)
  def getTotalRegisteredUsers(): Future[Int] =
      userAccountRepo.size()

  def newVIPAcc(vip: VIPUser): Future[Int] =
  	vipUserRepo.add(vip)
  def addUserWallet(wallet: UserAccountWallet): Future[Int] = userWalletRepo.add(wallet)
  // def walletExists(id: UUID): Future[Boolean] = userWalletRepo.exists(id)
  def getUserAccountWallet(id: UUID): Future[Option[UserAccountWallet]] = userWalletRepo.getByID(id)
  // def getUserAccountWalletHistory(id: UUID, limit: Int, offset: Int): Future[Seq[UserAccountWalletHistory]] =
  // userWalletHistoryRepo.getByAccountID(id, limit,offset)
  def getUserAccountWalletHistory(id: UUID, limit: Int, offset: Int): Future[PaginatedResult[UserAccountWalletHistory]] = {
    val currentPage: Int = limit * offset
    for {
      tasks <- userWalletHistoryRepo.getByAccountID(id, limit, currentPage)
      size <- userWalletHistoryRepo.getTotalSizeByID(id)
      toPaginate <- paginatedResult(tasks, size, limit, offset)
    } yield toPaginate
  }

  def addWalletBalance(wallet: UserAccountWallet, symbol: String, amount: BigDecimal): Future[Int] = {
    val updatedCoin = wallet.wallet.map { case coin@Coin(addr, s, am) => if (symbol == s) Coin(s, (am + amount))  else coin }
    val updatedWallet = wallet.copy(wallet = updatedCoin)
    userWalletRepo.update(updatedWallet)
  }
  def deductWalletBalance(wallet: UserAccountWallet, symbol: String, amount: BigDecimal): Future[Int] = {
    val updatedCoin = wallet.wallet.map { case coin@Coin(addr, s, am) => if (symbol == s) Coin(s, (am - amount))  else coin }
    val updatedWallet = wallet.copy(wallet = updatedCoin)
    userWalletRepo.update(updatedWallet)
  }
  // if returns 0 value throw an error..
  def getGameQuantityAmount(symbol: String, quantity: Int): Future[BigDecimal] = {
    httpSupport.getCurrentPriceBasedOnMainCurrency(symbol).map(_ * quantity)
  }
  // amount must be done converted to its currency value
  def hasEnoughBalanceByCurrency(wallet: UserAccountWallet, symbol: String, amount: BigDecimal): Boolean =
    wallet.wallet.find(_.symbol == symbol).map(_.amount >= amount).getOrElse(false)

  def saveUserWalletHistory(v: UserAccountWalletHistory): Future[Int] = userWalletHistoryRepo.add(v)
  def updateWithWithdrawCoin(id: UUID, coin: CoinWithdraw): Future[Int] = {
    for {
      // check if wallet has enough balance..
      hasWallet <- getUserAccountWallet(id)
      // check balances
      process <- {
        hasWallet.map { account =>
          val destination: String = coin.receiver.address.getOrElse("")
          val symbol: String = coin.receiver.symbol
          val amount: BigDecimal = coin.receiver.amount

          symbol match {
            case "USDC" =>
              val gasPrice: BigDecimal = (coin.gasPrice * DEFAULT_WEI_VALUE)
              val toDeductAmount: BigDecimal = gasPrice + amount
              if (hasEnoughBalanceByCurrency(account, symbol, toDeductAmount))
                httpSupport
                  .walletWithdrawUSDC(id, destination, amount, coin.gasPrice)
                  .map(_.getOrElse(0))
              else Future(0)

            case "ETH" =>
              val gasPrice: BigDecimal = (coin.gasPrice * DEFAULT_WEI_VALUE)
              val toDeductAmount: BigDecimal = gasPrice + amount
              if (hasEnoughBalanceByCurrency(account, symbol, toDeductAmount))
                httpSupport
                  .walletWithdrawETH(id, destination, amount, coin.gasPrice)
                  .map(_.getOrElse(0))
              else Future(0)
            // no BTC withdraw support for now..
            case "BTC" => Future(0)

            case _ => Future(0)
          }
        }.getOrElse(Future(0))
      }
    } yield (process)
  }
  def updateWithDepositCoin(id: UUID, coin: CoinDeposit): Future[Int] = {
    for {
      // chechk if tx already exists by txHash
      isTxHashExists <- userWalletHistoryRepo.existByTxHash(coin.txHash)
      // check transaction details using tx_hash
      process <- {
        if (!isTxHashExists) {
          for {
            isDeposited <- httpSupport.walletDeposit(id,
                                                    coin.txHash,
                                                    coin.issuer.address.getOrElse(""),
                                                    coin.receiver.address.getOrElse(""),
                                                    coin.receiver.symbol,
                                                    coin.receiver.amount)
            // if tx has failed for unknown reason
            _ <- {
              if (isDeposited.getOrElse(0) == 0)
                failedCoinDepositRepo.add(FailedCoinDeposit(coin.txHash, id, coin.issuer, coin.receiver))
              else Future.successful(())
            }
          } yield (isDeposited.getOrElse(0))
        }
        else Future(0)
      }
    } yield (process)
  }
}