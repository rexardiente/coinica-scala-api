package play.api.libs.mailer

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.util.Random
import scala.concurrent.Future
import play.api.libs.mailer._
import org.apache.commons.mail.EmailAttachment
import utils.SystemConfig
import models.domain.UserAccount

@Singleton
class MailerService @Inject()(mailerClient: MailerClient) {
  def sendAddOrUpdateEmailAddres(accountID: UUID, username: String, address: String, session: (String, Long), isUpdate: Boolean): String = {
    // default constructors
    val mailerAddress: String = SystemConfig.MAILER_ADDRESS
    val url: String = SystemConfig.MAILER_HOST
    // compose code for email link
    val code: String = s"${session._1}_${session._2}"
    val codeURL: String = s"${url}/donut/api/v1/user/email/confirm?id=${accountID}&email=${address}&code=${code}"
    val emailBody: String = views.html.emailConfirmationTemplate.render(username, code, codeURL, isUpdate).toString()
    val email: Email = new Email(
      "ACCOUNT EMAIL VERIFICATION",
      s"Coinica Support <${mailerAddress}>",
      Seq(address),
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

  def sendResetPasswordEmail(accountID: UUID, username: String, address: String, session: (String, Long)): String = {
    // default constructors
    val mailerAddress: String = SystemConfig.MAILER_ADDRESS
    val url: String = SystemConfig.MAILER_HOST
    // compose code for email link
    // sample code ~> ==tokenb2fc31ce-b683-46af-bs1b2-5c579aea39df_123123123213
    val code: String = s"${session._1}_${session._2}"
    val codeURL: String = s"${url}/donut/api/v1/user/password/reset/confirm?id=${accountID}&code=${code}"
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