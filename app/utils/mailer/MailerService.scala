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
    val code = s"${password}${expiration}_${newEmail}_${username}"
    val codeURL = s"http://127.0.0.1:9000/donut/api/v1/user/email/confirm?code=${code}"

    // compose bodu of the email in HTML format...
    // val emailBody: String = s"""<html><body><p><h3>Please confirm the email below.</h3></p><p><h3>URL: $code</h3></p></body></html>"""
    val emailBody: String = s"""
    <!DOCTYPE html>
			<html xmlns="http://www.w3.org/1999/xhtml" lang="en-GB">
			<head>
			  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
			  <title>EGS Email Confirmation</title>
			  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>

			  <style type="text/css">
			    a[x-apple-data-detectors] {color: inherit !important;}
			  </style>

			</head>
			<body style="margin: 0; padding: 0;">
			  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
			    <tr>
			      <td style="padding: 20px 0 30px 0;">

			<table align="center" border="0" cellpadding="0" cellspacing="0" width="900" style="border-collapse: collapse;">
			  <tr>
			    <td align="center" bgcolor="#08113f" style="padding: 40px 0 30px 0;">
			      <img src="https://i.imgur.com/XenHdY3.jpg" alt="Creating Email Magic." width="300" height="230" style="display: block;" />
			    </td>
			  </tr>
			  <tr>
			    <td bgcolor="#ffffff" style="padding: 40px 30px 40px 30px;">
			      <table border="0" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse;">
			        <!-- <tr>
			          <td style="color: #153643; font-family: Arial, sans-serif;">
			            <h1 style="font-size: 24px; margin: 0;">Lorem ipsum dolor sit amet!</h1>
			          </td>
			        </tr>
			        <tr>
			          <td style="color: #153643; font-family: Arial, sans-serif; font-size: 16px; line-height: 24px; padding: 20px 0 30px 0;">
			            <p style="margin: 0;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. In tempus adipiscing felis, sit amet blandit ipsum volutpat sed. Morbi porttitor, eget accumsan dictum, nisi libero ultricies ipsum, in posuere mauris neque at erat.</p>
			          </td>
			        </tr> -->
			        <tr>
			          <td style="color: #153643; font-family: Arial, sans-serif;">
			          	<!-- Confirm Your Email! -->
			            <h1 style="font-size: 24px; margin: 0;">Hi ${username.toUpperCase}, Confirm Your Email!</h1>
			          </td>
			        </tr>
			        <tr>
			          <td style="color: #153643; font-family: Arial, sans-serif; font-size: 16px; line-height: 24px; padding: 20px 0 30px 0;">
			            <p style="margin: 0;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. In tempus adipiscing felis, sit amet blandit ipsum volutpat sed. Morbi porttitor, eget accumsan dictum, nisi libero ultricies ipsum, in posuere mauris neque at erat.</p>
			          </td>
			        </tr>
			        <tr>
			          <td>
			            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse;">
			              <tr>
			                <td width="100%" valign="top">
			                  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse;">
			                    <tr>
			                      <td>
			                        <img src="https://i.imgur.com/cerMIlg.png" alt="" width="70%" style="display: block; margin-right: auto; margin-right: auto;" />
			                        <p style="margin: 0; padding-top: 20px;"> Here's your confirmation link: <a href="${codeURL}">${code}</a> </p>
			                      </td>
			                    </tr>
			                  </table>
			                </td>
			              </tr>
			            </table>
			          </td>
			        </tr>
			      </table>
			    </td>
			  </tr>
			  <tr>
			    <td bgcolor="#ee4c50" style="padding: 30px 30px;">
			        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse;">
			        <tr>
			          <td style="color: #ffffff; font-family: Arial, sans-serif; font-size: 14px;">
			            <p style="margin: 0;">&reg; 2020-2021 <a href="http://3.34.146.80:5000" style="color: #ffffff;">EOS game store</a>, All rights reserved</p>
			          </td>
			          <td align="right">
			            <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
			              <tr>
			                <td>
			                  <a href="http://www.twitter.com/">
			                    <img src="https://assets.codepen.io/210284/tw.gif" alt="Twitter." width="38" height="38" style="display: block;" border="0" />
			                  </a>
			                </td>
			                <td style="font-size: 0; line-height: 0;" width="20">&nbsp;</td>
			                <td>
			                  <a href="http://www.twitter.com/">
			                    <img src="https://assets.codepen.io/210284/fb.gif" alt="Facebook." width="38" height="38" style="display: block;" border="0" />
			                  </a>
			                </td>
			              </tr>
			            </table>
			          </td>
			        </tr>
			      </table>
			    </td>
			  </tr>
			</table>

			      </td>
			    </tr>
			  </table>
			</body>
		</html>
    """

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
    mailerClient.send(email)
  }

}