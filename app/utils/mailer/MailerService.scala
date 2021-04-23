package play.api.libs.mailer

import javax.inject.Inject
import java.io.File
import java.time.Instant
import scala.concurrent.Future
import play.api.libs.mailer._
import org.apache.commons.mail.EmailAttachment
import utils.Config
import models.domain.UserAccount

class MailerService @Inject()(mailerClient: MailerClient) {
  def sendEmail(account: UserAccount, newEmail: String): Future[String] = Future.successful {
  	// default constructors
  	val mailerAddress: String = Config.MAILER_ADDRESS
  	val mailExpiration: Int = Config.MAIL_EXPIRATION
  	// compose code for email link
  	val username: String = account.username
    val password: String = account.password
    // Set expiration time to CONFIG TIME LIMIT * 60 sec
    val expiration: Long = Instant.now.getEpochSecond + (60 * mailExpiration)
    val code: String = s"${password}${expiration}_${newEmail}_${username}"
    val codeURL: String = s"http://127.0.0.1:9000/donut/api/v1/user/email/confirm?code=${code}"
    // compose body of the email in template and render as String
    // https://stackoverflow.com/questions/12538368/email-templates-as-scala-templates-in-play/12543639
    val emailBody: String = views.html.emailConfirmationTemplate.render(username, code, codeURL).toString()
  	val email: Email = new Email(
      "EGS EMAIL CONFIRMATION",
      mailerAddress,
      Seq(newEmail),
      Some("EGS CONFIRMATION EMAIL"),
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