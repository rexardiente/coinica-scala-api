package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.{ Instant, LocalDate, ZoneId, ZoneOffset }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import models.domain.{ PaginatedResult, UserAccount, VIPUser, UserToken, UserAccountWallet }
import models.domain.wallet.support._
import models.repo.{ UserAccountRepo, VIPUserRepo, UserTokenRepo, UserAccountWalletRepo, UserAccountWalletHistoryRepo }
import utils.lib.{ MultiCurrencyHTTPSupport, EOSIOHTTPSupport }

@Singleton
class UserAccountService @Inject()(
      userAccountRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      userTokenRepo: UserTokenRepo,
      userWalletRepo: UserAccountWalletRepo,
      userWalletHistoryRepo: UserAccountWalletHistoryRepo,
      httpSupport: MultiCurrencyHTTPSupport,
      eosioSupport: EOSIOHTTPSupport) {
  def isExist(name: String): Future[Boolean] =
  	userAccountRepo.exist(name)

  def getAccountByID(id: UUID): Future[Option[UserAccount]] =
  	userAccountRepo.getByID(id)

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

  def addBalanceByCurrency(id: UUID, currency: String, totalAmount: BigDecimal): Future[Int] = {
    for {
      hasWallet <- getUserAccountWallet(id)
      process <- {
        hasWallet.map { wallet =>
          val updatedBalance: UserAccountWallet = currency match {
            case "ETH" =>
              val newBalance: BigDecimal = wallet.eth.amount + totalAmount
              wallet.copy(eth=Coin("ETH", newBalance))
            case "USDC" =>
              val newBalance: BigDecimal = wallet.usdc.amount + totalAmount
              wallet.copy(usdc=Coin("USDC", newBalance))
          }
          userWalletRepo.update(updatedBalance)
        }.getOrElse(Future(0))
      }
    } yield (process)
  }
  // before deduction, make sure amount is already final
  // if deposit -> (gasPrice, wei and amount) = totalAmount
  // case "USDC" =>
  //   val newBalance: BigDecimal = account.usdc.amount - ((gasPrice * 0.000000000000000001) + totalAmount)
  //   userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))
  // case "ETH" =>
  //   val newBalance: BigDecimal = account.eth.amount - (((500000 * gasPrice) * 0.000000000000000001) + totalAmount)
  //   userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))
  // gasPrice: Int
  def deductBalanceByCurrency(id: UUID, currency: String, totalAmount: BigDecimal): Future[Int] = {
    for {
      hasWallet <- getUserAccountWallet(id)
      process <- {
        hasWallet.map { wallet =>
          val updatedBalance: UserAccountWallet = currency match {
            case "ETH" =>
              val newBalance: BigDecimal = wallet.eth.amount - totalAmount
              wallet.copy(eth=Coin("ETH", newBalance))
            case "USDC" =>
              val newBalance: BigDecimal = wallet.usdc.amount - totalAmount
              wallet.copy(usdc=Coin("USDC", newBalance))
          }
          userWalletRepo.update(updatedBalance)
        }.getOrElse(Future(0))
      }
    } yield (process)
  }

  def thGameStart(id: UUID, gameID: Int, currency: String, quantity: Int): Future[Int] = {
    for {
      hasWallet <- getUserAccountWallet(id)
      currentValue <- getGameQuantityAmount(currency, quantity)
      // check if has enough balance..
      hasEnoughBalance <- Future.successful {
        hasWallet
          .map(v => hasEnoughBalanceByCurrency(v, currency, currentValue))
          .getOrElse(false)
      }
      // if has enough balance proceed, else do nothing..
      // send tx on smartcontract
      initGame <- {
        if (hasEnoughBalance) eosioSupport.treasureHuntGameStart(gameID, quantity)
        else Future(false)
      }
      // deduct balance on the account
      updateBalance <- {
        if (initGame) deductBalanceByCurrency(id, currency, currentValue)
        else Future(0)
      }
    } yield (updateBalance)
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
          coin.receiver.currency match {
            case "USDC" =>
              if (account.usdc.amount >= (coin.gasPrice * 0.000000000000000001) + coin.receiver.amount)
                httpSupport
                  .walletWithdrawUSDC(id, coin.receiver.address.getOrElse(""), coin.receiver.amount, 500000 * coin.gasPrice)
                  .map(_.getOrElse(0))
              else Future(0)
            case "ETH" =>
              if (account.eth.amount >= ((500000 * coin.gasPrice) * 0.000000000000000001) + coin.receiver.amount)
                httpSupport
                  .walletWithdrawETH(id, coin.receiver.address.getOrElse(""), coin.receiver.amount, coin.gasPrice)
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
      isTxHashExists <- userWalletHistoryRepo.existByTxHashAndID(coin.txHash, id)
      // check transaction details using tx_hash
      txDetails <- httpSupport.getETHTxInfo(coin.txHash, coin.receiver.currency)
      process <- {
        if (!isTxHashExists) {
          for {
            // get account balances using ID and validate data.. (account and amount)
            hasWallet <- getUserAccountWallet(id)
            update <- {
              hasWallet.map { account =>
                // update account  balance..
                txDetails.map { details =>
                  val result: ETHJsonRpcResult = details.result
                  // check tx request and response details...
                  if (result.from == coin.issuer.address.getOrElse("") && result.to == coin.receiver.address.getOrElse("")) {
                    // check if type of currency to update
                    coin.receiver.currency match {
                      case "USDC" | "ETH" =>
                        for {
                          saveToDB <- saveUserWalletHistory(coin.toWalletHistory(id, "DEPOSIT", details.result))
                          updateBalance <- addBalanceByCurrency(id, coin.receiver.currency, result.value.toDouble)
                        } yield (updateBalance)
                      case _ => Future(0)
                    }
                  }
                  else Future(0)
                }.getOrElse(Future(0))
              }.getOrElse(Future(0))
            }
          } yield (update)
        } else Future(0)
      }
    } yield (process)
  }
}