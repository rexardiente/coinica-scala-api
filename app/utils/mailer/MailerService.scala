package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.util.Random
import scala.concurrent.Future
import play.api.libs.mailer._
import org.apache.commons.mail.EmailAttachment
import utils.Config
import models.domain.UserAccount

@Singleton
class MailerService @Inject()(mailerClient: MailerClient) {
  def sendAddEmailAddress(account: UserAccount, newEmail: String): Future[String] = Future.successful {
  	// default constructors
  	val mailerAddress: String = Config.MAILER_ADDRESS
    val url: String = Config.MAILER_HOST
    val protocol: String = Config.PROTOCOL
  	// compose code for email link
  	val username: String = account.username
    val password: String = account.password
    // Set expiration time to CONFIG TIME LIMIT * 60 sec
    // val expiration: Long = Instant.now.getEpochSecond + (60 * mailExpiration)
    val randomString: String  = Random.alphanumeric.dropWhile(_.isDigit).take(Config.MAIL_RANDOM_CODE_LIMIT).mkString
    val code: String = s"${password}${randomString}_${newEmail}_${username}"
    val codeURL: String = s"${protocol}://${url}/donut/api/v1/user/email/confirm?code=${code}"
    val isUpdate: Boolean = account.email.map(_ => true).getOrElse(false)
    // compose body of the email in template and render as String
    // https://stackoverflow.com/questions/12538368/email-templates-as-scala-templates-in-play/12543639
    val emailBody: String = views.html.emailConfirmationTemplate.render(username, code, codeURL, isUpdate).toString()
  	val email: Email = new Email(
      "ACCOUNT EMAIL VERIFICATION",
      s"Coinica Support <${mailerAddress}>",
      Seq(newEmail),
      Some("Email Verification"),
      Some(emailBody),
      None,
      Seq.empty,
      Seq.empty,
      Seq.empty,
      Some(mailerAddress),
      Seq.empty,
      Seq.empty)
  	// send email
    mailerClient.send(email)
  }
  // def sendUpdateEmailAddress(account: UserAccount, newEmail: String): Future[String] = Future.successful {
  //   // default constructors
  //   val mailerAddress: String = Config.MAILER_ADDRESS
  //   val url: String = Config.MAILER_HOST
  //   val protocol: String = Config.PROTOCOL
  //   // compose code for email link
  //   val username: String = account.username
  //   val password: String = account.password
  //   // Set expiration time to CONFIG TIME LIMIT * 60 sec
  //   // val expiration: Long = Instant.now.getEpochSecond + (60 * mailExpiration)
  //   val randomString: String  = Random.alphanumeric.dropWhile(_.isDigit).take(Config.MAIL_RANDOM_CODE_LIMIT).mkString
  //   val code: String = s"${password}${randomString}_${newEmail}_${username}"
  //   val codeURL: String = s"${protocol}://${url}/donut/api/v1/user/email/confirm?code=${code}"
  //   // compose body of the email in template and render as String
  //   // https://stackoverflow.com/questions/12538368/email-templates-as-scala-templates-in-play/12543639
  //   val emailBody: String = views.html.emailConfirmationTemplate.render(username, code, codeURL).toString()
  //   val email: Email = new Email(
  //     "EMAIL VERIFICATION",
  //     mailerAddress,
  //     Seq(account.email.getOrElse("")),
  //     Some("EMAIL VERIFICATION"),
  //     Some(emailBody),
  //     None,
  //     Seq.empty,
  //     Seq.empty,
  //     Seq.empty,
  //     Some(mailerAddress),
  //     Seq.empty,
  //     Seq.empty)
  //   // send email
  //   mailerClient.send(email)
  // }
  // def sendResetPasswordEmail(account: UserAccount, address: String): String = {
  //   // default constructors
  //   val mailerAddress: String = Config.MAILER_ADDRESS
  //   val url: String = Config.MAILER_HOST
  //   val protocol: String = Config.PROTOCOL
  //   // compose code for email link
  //   val username: String = account.username
  //   val password: String = account.password
  //   // Set expiration time to CONFIG TIME LIMIT * 60 sec
  //   // val expiration: Long = Instant.now.getEpochSecond + (60 * mailExpiration)
  //   val randomString: String  = Random.alphanumeric.dropWhile(_.isDigit).take(Config.MAIL_RANDOM_CODE_LIMIT).mkString
  //   val code: String = s"${password}${randomString}_${username}"
  //   val codeURL: String = s"${protocol}://${url}/donut/api/v1/user/password/reset/confirm?code=${code}"
  //   // compose body of the email in template and render as String
  //   // https://stackoverflow.com/questions/12538368/email-templates-as-scala-templates-in-play/12543639
  //   val emailBody: String = views.html.resetPasswordEmailConfirmation.render(username, code, codeURL).toString()
  //   val email: Email = new Email(
  //     "RESET ACCOUNT PASSWORD",
  //     s"Coinica Support <${mailerAddress}>",
  //     Seq(address),
  //     Some("Reset Password"),
  //     Some(emailBody),
  //     None,
  //     Seq.empty,
  //     Seq.empty,
  //     Seq.empty,
  //     Some(mailerAddress),
  //     Seq.empty,
  //     Seq.empty)
  //   // send email
  //   mailerClient.send(email)
  // }
  def sendResetPasswordEmail(accountID: UUID, username: String, address: String, session: (String, Long)): String = {
    // default constructors
    val mailerAddress: String = Config.MAILER_ADDRESS
    val url: String = Config.MAILER_HOST
    val protocol: String = Config.PROTOCOL
    // compose code for email link
    // sample code ~> ==tokenb2fc31ce-b683-46af-bs1b2-5c579aea39df_123123123213
    val code: String = s"${session._1}_${session._2}"
    val codeURL: String = s"${protocol}://${url}/donut/api/v1/user/password/reset/confirm?id=${accountID}&code=${code}"
    // compose body of the email in template and render as String
    // https://stackoverflow.com/questions/12538368/email-templates-as-scala-templates-in-play/12543639
    val emailBody: String = views.html.resetPasswordEmailConfirmation.render(username, code, codeURL).toString()
    val email: Email = new Email(
      "RESET ACCOUNT PASSWORD",
      s"Coinica Support <${mailerAddress}>",
      Seq(address),
      Some("Reset Password"),
      Some(emailBody),
      None,
      Seq.empty,
      Seq.empty,
      Seq.empty,
      Some(mailerAddress),
      Seq.empty,
      Seq.empty)
    // send email
    mailerClient.send(email)
  }

}