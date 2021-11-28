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

  // def isEmailExist(email: String): Future[Boolean] =
  //   userAccountRepo.isEmailExist(email)

  // def addOrUpdateEmailAccount(id: UUID, email: String): Future[Int] = {
  //   for {
  //     isExists <- isEmailExist(email)
  //     // get acccount based on first validations, proceed adding or updating email
  //     account <- getAccountByID(id)
  //     process <- {
  //       // check if email not associated with any accounts and account exists
  //       if (!isExists && account != None) {
  //         try {
  //           val updatedAccount: UserAccount = account.get.copy(email = Some(email), isVerified = true)
  //           updateUserAccount(updatedAccount)
  //         } catch {
  //           case _: Throwable => Future(0)
  //         }
  //       }
  //       else Future(0)
  //     }
  //   } yield (process)
  // }

  def newVIPAcc(vip: VIPUser): Future[Int] =
  	vipUserRepo.add(vip)

  // def getUserAccountByIDAndToken(id: UUID, token: String): Future[Option[UserAccount]] = {
  //   for {
  //     // check if token exists on DB..
  //      hasValidToken <- userTokenRepo.getLoginByIDAndToken(id, token)
  //      // validate
  //      processed <- {
  //        if (hasValidToken != None) getAccountByID(hasValidToken.map(_.id).getOrElse(UUID.randomUUID))
  //        else Future(None)
  //      }
  //   } yield (processed)
  // }
  // def updateUserToken(user: UserToken): Future[Int] =
  //   userTokenRepo.update(user)
  // def getUserTokenByID(id: UUID): Future[Option[UserToken]] =
  //   userTokenRepo.getByID(id)

  // def addUpdateUserToken(user: UserToken): Future[Int] = {
  //   for {
  //     exists <- userTokenRepo.exists(user.id)
  //     process <- {
  //       if (exists) userTokenRepo.update(user)
  //       else userTokenRepo.add(user)
  //     }
  //   } yield (process)
  // }
  // def removePasswordTokenByID(id: UUID): Future[Int] =
  //   for {
  //     token <- userTokenRepo.getByID(id)
  //     process <- {
  //       if (token != None) userTokenRepo.update(token.get.copy(password = None))
  //       else Future(0)
  //     }
  //   } yield (process)
  // def removeEmailTokenByID(id: UUID): Future[Int] =
  //   for {
  //     token <- userTokenRepo.getByID(id)
  //     process <- {
  //       if (token != None) userTokenRepo.update(token.get.copy(email = None))
  //       else Future(0)
  //     }
  //   } yield (process)

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

  def addBalanceByCurrency(id: UUID, symbol: String, amount: BigDecimal): Future[Int] = {
    for {
      optWallet <- getUserAccountWallet(id)
      process <- Future.successful {
        optWallet.map { wallet =>
          wallet.wallet
                .find(_.symbol == symbol)
                .map { coin =>
                  val updatedCoin = Coin(coin.symbol, coin.amount + amount)
                  // remove old record then replace the new updated coin
                  wallet.copy(wallet = (wallet.wallet.filter(_.symbol != symbol) :+ updatedCoin))
                }
                .getOrElse(wallet)
        }
      }
      updateBalance <- process.map(userWalletRepo.update(_)).getOrElse(Future(0))
    } yield (updateBalance)
  }
  // before deduction, make sure amount is already final
  // if deposit -> (gasPrice, wei and amount) = amount
  // case "USDC" =>
  //   val newBalance: BigDecimal = account.usdc.amount - ((gasPrice * DEFAULT_WEI_VALUE) + amount)
  //   userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))
  // case "ETH" =>
  //   val newBalance: BigDecimal = account.eth.amount - (((500000 * gasPrice) * DEFAULT_WEI_VALUE) + amount)
  //   userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))
  // gasPrice: Int
  def deductBalanceByCurrency(id: UUID, symbol: String, amount: BigDecimal): Future[Int] = {
    for {
      optWallet <- getUserAccountWallet(id)
      process <- Future.successful {
        optWallet.map { wallet =>
          wallet.wallet
                .find(_.symbol == symbol)
                .map { coin =>
                  val updatedCoin = Coin(coin.symbol, coin.amount - amount)
                  // remove old record then replace the new updated coin
                  wallet.copy(wallet = (wallet.wallet.filter(_.symbol != symbol) :+ updatedCoin))
                }
                .getOrElse(wallet)
          }
      }
      updateBalance <- process.map(userWalletRepo.update(_)).getOrElse(Future(0))
    } yield (updateBalance)
  }
  // 1 quantity = 1 game token
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