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

@Singleton
class UserAccountService @Inject()(
      userAccountRepo: UserAccountRepo,
      vipUserRepo: VIPUserRepo,
      userTokenRepo: UserTokenRepo,
      userWalletRepo: UserAccountWalletRepo,
      userWalletHistoryRepo: UserAccountWalletHistoryRepo,
      httpSupport: utils.lib.MultiCurrencyHTTPSupport) {
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

  def addBalanceByCurrency(id: UUID, currency: String, amount: Double): Future[Int] = {
    for {
      hasAccount <- getUserAccountWallet(id)
      process <- {
        hasAccount.map { account =>
          currency match {
            case "USDC" =>
              val newBalance: Double = account.usdc.amount + amount
              userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))
            case "ETH" =>
              val newBalance: Double = account.eth.amount + amount
              userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))
            case _ =>
              Future(0)
          }
        }.getOrElse(Future(0))
      }
    } yield (process)
  }

  def deductBalanceByCurrency(id: UUID, currency: String, amount: Double, fee: Long): Future[Int] = {
    for {
      hasAccount <- getUserAccountWallet(id)
      process <- {
        hasAccount.map { account =>
          currency match {
            case "USDC" =>
              val newBalance: Double = account.usdc.amount - (amount + fee)
              userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))
            case "ETH" =>
              val newBalance: Double = account.eth.amount - (amount + fee)
              userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))
            case _ =>
              Future(0)
          }
        }.getOrElse(Future(0))
      }
    } yield (process)
  }

  def saveUserWalletHistory(history: UserAccountWalletHistory): Future[Int] =
    userWalletHistoryRepo.add(history)
  def updateWithWithdrawCoin(id: UUID, coin: CoinWithdraw): Future[Int] = {
    for {
      // check if wallet has enough balance..
      hasAccount <- getUserAccountWallet(id)
      // check balances
      process <- {
        hasAccount.map { account =>
          coin.receiver.currency match {
            case "USDC" =>
              if (account.usdc.amount >= (coin.receiver.amount + (coin.fee * 0.000000000000000001)))
                httpSupport
                  .walletWithdrawUSDC(id, coin.receiver.address.getOrElse(""), coin.receiver.amount, coin.fee)
                  .map(_.getOrElse(0))
              else Future(0)
            case "ETH" =>
              if (account.eth.amount >= (coin.receiver.amount + (coin.fee * 0.000000000000000001)))
                httpSupport
                  .walletWithdrawETH(id, coin.receiver.address.getOrElse(""), coin.receiver.amount, coin.fee)
                  .map(_.getOrElse(0))
              else Future(0)
            // case "BTC" =>
            //   if (account.btc.amount >= (coin.receiver.amount + coin.fee)) Future(1)
            //   else Future(0)
            case _ => Future(0)
          }
        }.getOrElse(Future(0))
      }
    } yield (process)
  }
  // def updateWithWithdrawCoin(id: UUID, coin: CoinWithdraw): Future[Int] = {
  //   for {
  //     // check if wallet has enough balance..
  //     hasAccount <- getUserAccountWallet(id)
  //     // check balances
  //     hasEnoughBalance <- Future.successful {
  //       hasAccount.map { account =>
  //         coin.receiver.currency match {
  //           case "USDC" =>
  //             if (account.usdc.amount >= (coin.receiver.amount + (coin.fee * 0.000000000000000001))) true
  //             else false
  //           case _ => false
  //         }
  //       }.getOrElse(false)
  //     }
  //     // transfer using Node API, and validate response..
  //     transfer <- {
  //       if (hasEnoughBalance)
  //         coin.receiver.currency match {
  //           case "USDC" =>
  //             httpSupport
  //               .walletWithdrawUSDC(coin.receiver.address.getOrElse(""), coin.receiver.amount, coin.fee)
  //               .map((_, coin.receiver.currency))
  //           case _ =>
  //             Future((None, coin.receiver.currency))
  //         }
  //       else Future((None, coin.receiver.currency))
  //     }
  //     // check transaction details using tx_hash
  //     txDetails <- {
  //       if (transfer._1 != None)
  //         httpSupport.getETHTxInfo(transfer._1.getOrElse(""), transfer._2)
  //       else Future(None)
  //     }
  //     updateBalance <- {
  //       println("txDetails", txDetails)
  //       if (txDetails != None) {
  //         hasAccount.map { account =>
  //           // update account  balance..
  //           val result: ETHJsonRpcResult = txDetails.get.result
  //           // check tx request and response details...
  //           if (result.to == coin.receiver.address.getOrElse("")) {
  //             // check if type of currency to update
  //             coin.receiver.currency match {
  //               case "BTC" =>
  //                 val newBalance: Double = account.btc.amount - result.value.toDouble
  //                 userWalletRepo.update(account.copy(btc=Coin("BTC", newBalance)))

  //               case "USDC" =>
  //                 val newBalance: Double = account.usdc.amount - result.value.toDouble
  //                 userWalletRepo.update(account.copy(usdc=Coin("USDC", newBalance)))

  //               case "ETH" =>
  //                 val newBalance: Double = account.eth.amount - result.value.toDouble
  //                 userWalletRepo.update(account.copy(eth=Coin("ETH", newBalance)))

  //               case _ => Future(0)
  //             }
  //           } else Future(0)
  //         }.getOrElse(Future(0))
  //       }
  //       else Future(0)
  //     }
  //     _ <- Future.successful {
  //       if (transfer._1 != None && txDetails != None)
  //         saveUserWalletHistory(coin.toWalletHistory(transfer._1.getOrElse(""), id, "WITHDRAW", txDetails.get.result))
  //     }
  //   } yield (updateBalance)
  // }
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
            hasAccount <- getUserAccountWallet(id)
            update <- {
              hasAccount.map { account =>
                // update account  balance..
                txDetails.map { details =>
                  val result: ETHJsonRpcResult = details.result
                  // check tx request and response details...
                  if (result.from == coin.issuer.address.getOrElse("") && result.to == coin.receiver.address.getOrElse("")) {
                    // check if type of currency to update
                    coin.receiver.currency match {
                      case "USDC" | "ETH" =>
                        addBalanceByCurrency(id, coin.receiver.currency, result.value.toDouble)
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
      // if success insert history else do nothing,. Neeed enhancements..
      _ <- Future.successful(txDetails.map(_ => saveUserWalletHistory(coin.toWalletHistory(id, "DEPOSIT", txDetails.get.result))))
    } yield (process)
  }
}