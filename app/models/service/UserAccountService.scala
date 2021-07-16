package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneId, ZoneOffset }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain.{ PaginatedResult, UserAccount, VIPUser, UserToken, UserAccountWallet }
import models.domain.wallet.support._
import models.repo.{
                UserAccountRepo,
                VIPUserRepo,
                UserTokenRepo,
                UserAccountWalletRepo,
                UserAccountWalletHistoryRepo,
                FailedCoinDepositRepo }
import utils.lib.MultiCurrencyHTTPSupport
import utils.Config.DEFAULT_WEI_VALUE

@Singleton
class UserAccountService @Inject()(
      userAccountRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      userTokenRepo: UserTokenRepo,
      userWalletRepo: UserAccountWalletRepo,
      userWalletHistoryRepo: UserAccountWalletHistoryRepo,
      failedCoinDepositRepo: FailedCoinDepositRepo,
      httpSupport: MultiCurrencyHTTPSupport) {
  def isExist(name: String): Future[Boolean] =
  	userAccountRepo.exist(name)

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

  def isEmailExist(email: String): Future[Boolean] =
    userAccountRepo.isEmailExist(email)

  def addOrUpdateEmailAccount(id: UUID, email: String): Future[Int] = {
    for {
      isExists <- isEmailExist(email)
      // get acccount based on first validations, proceed adding or updating email
      account <- getAccountByID(id)
      process <- {
        // check if email not associated with any accounts and account exists
        if (!isExists && account != None) {
          try {
            val updatedAccount: UserAccount = account.get.copy(email = Some(email), isVerified = true)
            updateUserAccount(updatedAccount)
          } catch {
            case _: Throwable => Future(0)
          }
        }
        else Future(0)
      }
    } yield (process)
  }

  def newVIPAcc(vip: VIPUser): Future[Int] =
  	vipUserRepo.add(vip)

  def getUserAccountByIDAndToken(id: UUID, token: String): Future[Option[UserAccount]] = {
    for {
      // check if token exists on DB..
       hasValidToken <- userTokenRepo.getLoginByIDAndToken(id, token)
       // validate
       processed <- {
         if (hasValidToken != None) getAccountByID(hasValidToken.map(_.id).getOrElse(UUID.randomUUID))
         else Future(None)
       }
    } yield (processed)
  }
  def updateUserToken(user: UserToken): Future[Int] =
    userTokenRepo.update(user)
  def getUserTokenByID(id: UUID): Future[Option[UserToken]] =
    userTokenRepo.getByID(id)

  def addUpdateUserToken(user: UserToken): Future[Int] = {
    for {
      exists <- userTokenRepo.exists(user.id)
      process <- {
        if (exists) userTokenRepo.update(user)
        else userTokenRepo.add(user)
      }
    } yield (process)
  }
  def removePasswordTokenByID(id: UUID): Future[Int] =
    for {
      token <- userTokenRepo.getByID(id)
      process <- {
        if (token != None) userTokenRepo.update(token.get.copy(password = None))
        else Future(0)
      }
    } yield (process)
  def removeEmailTokenByID(id: UUID): Future[Int] =
    for {
      token <- userTokenRepo.getByID(id)
      process <- {
        if (token != None) userTokenRepo.update(token.get.copy(email = None))
        else Future(0)
      }
    } yield (process)

  def addUserWallet(wallet: UserAccountWallet): Future[Int] = userWalletRepo.add(wallet)
  def walletExists(id: UUID): Future[Boolean] = userWalletRepo.exists(id)
  def getUserAccountWallet(id: UUID): Future[Option[UserAccountWallet]] = userWalletRepo.getByID(id)
  def getUserAccountWalletHistory(id: UUID): Future[Seq[UserAccountWalletHistory]] =
    userWalletHistoryRepo.getByAccountID(id)

  def addBalanceByCurrency(id: UUID, currency: String, totalAmount: BigDecimal): Future[Int] = {
    for {
      hasWallet <- getUserAccountWallet(id)
      process <- Future.successful {
        hasWallet.map { wallet =>
          currency match {
            case "ETH" =>
              val newBalance: BigDecimal = (wallet.eth.amount + totalAmount)
              wallet.copy(eth=Coin("ETH", newBalance))
            case "USDC" =>
              val newBalance: BigDecimal = (wallet.usdc.amount + totalAmount)
              wallet.copy(usdc=Coin("USDC", newBalance))
          }
        }
      }
      updateBalance <- process.map(userWalletRepo.update(_)).getOrElse(Future(0))
    } yield (updateBalance)
  }
  // before deduction, make sure amount is already final
  // if deposit -> (gasPrice, wei and amount) = totalAmount
  // case "USDC" =>
  //   val newBalance: BigDecimal = account.usdc.amount - ((gasPrice * DEFAULT_WEI_VALUE) + totalAmount)
  //   userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))
  // case "ETH" =>
  //   val newBalance: BigDecimal = account.eth.amount - (((500000 * gasPrice) * DEFAULT_WEI_VALUE) + totalAmount)
  //   userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))
  // gasPrice: Int
  def deductBalanceByCurrency(id: UUID, currency: String, totalAmount: BigDecimal): Future[Int] = {
    for {
      hasWallet <- getUserAccountWallet(id)
      process <- {
        hasWallet.map { wallet =>
          val updatedBalance: UserAccountWallet = currency match {
            case "ETH" =>
              wallet.copy(eth=Coin("ETH", wallet.eth.amount - totalAmount))
            case "USDC" =>
              wallet.copy(usdc=Coin("USDC", wallet.usdc.amount - totalAmount))
          }
          userWalletRepo.update(updatedBalance)
        }.getOrElse(Future(0))
      }
    } yield (process)
  }
  // 1 quantity = 1 game token
  def getGameQuantityAmount(currency: String, quantity: Int): Future[BigDecimal] = {
    httpSupport.getCurrentPriceBasedOnMainCurrency(currency).map(_ * quantity)
  }
  // amount must be done converted to its currency value
  def hasEnoughBalanceByCurrency(wallet: UserAccountWallet, currency: String, amount: BigDecimal): Boolean = {
    val baseAmount: BigDecimal = currency match {
      case "ETH" => wallet.eth.amount
      case "USDC" => wallet.usdc.amount
      case _ => 0 // unknown currency
    }
    if (baseAmount >= amount) true else false
  }

  def saveUserWalletHistory(v: UserAccountWalletHistory): Future[Int] = userWalletHistoryRepo.add(v)
  def updateWithWithdrawCoin(id: UUID, coin: CoinWithdraw): Future[Int] = {
    for {
      // check if wallet has enough balance..
      hasWallet <- getUserAccountWallet(id)
      // check balances
      process <- {
        hasWallet.map { account =>
          val destination: String = coin.receiver.address.getOrElse("")
          val currency: String = coin.receiver.currency
          val amount: BigDecimal = coin.receiver.amount

          currency match {
            case "USDC" =>
              val gasPrice: BigDecimal = (coin.gasPrice * DEFAULT_WEI_VALUE)
              val toDeductAmount: BigDecimal = gasPrice + amount
              if (hasEnoughBalanceByCurrency(account, currency, toDeductAmount))
                httpSupport
                  .walletWithdrawUSDC(id, destination, amount, coin.gasPrice)
                  .map(_.getOrElse(0))
              else Future(0)

            case "ETH" =>
              val gasPrice: BigDecimal = (coin.gasPrice * DEFAULT_WEI_VALUE)
              val toDeductAmount: BigDecimal = gasPrice + amount
              if (hasEnoughBalanceByCurrency(account, currency, toDeductAmount))
                httpSupport
                  .walletWithdrawETH(id, destination, amount, coin.gasPrice)
                  .map(_.getOrElse(0))
              else Future(0)

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
                                                    coin.receiver.currency,
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