// package models.domain

// import java.time.{ Duration, Instant }
// import scala.concurrent.{ ExecutionContext, Future }
// import ejisan.scalauthx.{ HashedCredential, HashedCredentialFactory }
// import ejisan.kuro.otp.{ TOTP, OTPAlgorithm, OTPKey }

// case class Security(
//     hashedPassword: HashedCredential,
//     verificationInitialTime: Option[Instant],
//     resetPasswordCode: Option[OTPKey],
//     newEmail: Option[String],
//     newEmailCode: Option[OTPKey],
//     disabledAt: Option[Instant]) {

//   def isEmailVerified: Boolean  = newEmailCode.isEmpty

//   def isDisabled: Boolean = disabledAt.isDefined

//   def validatePassword(password: String)(implicit ec: ExecutionContext): Future[Boolean] = {
//     Security.hasher.verify(password, hashedPassword)
//   }

//   def validateEmail(code: String, timeout: Duration): Boolean =
//     emailTotp(timeout).map(_.validate(code)).getOrElse(false)

//   def generateEmailCode(timeout: Duration): String =
//     emailTotp(timeout).map(_.generate()).getOrElse("")

//   def validateResetPassword(code: String, timeout: Duration): Boolean =
//     ressetPasswordTotp(timeout).map(_.validate(code)).getOrElse(false)

//   def generateResetPasswordCode(timeout: Duration): String =
//     ressetPasswordTotp(timeout).map(_.generate()).getOrElse("")

//   def emailTotp(timeout: Duration): Option[TOTP] = {
//     newEmailCode.flatMap { secret =>
//       verificationInitialTime.map { initialTime =>
//         TOTP(
//           OTPAlgorithm.SHA1,
//           6,
//           timeout.getSeconds.toInt,
//           initialTime.getEpochSecond,
//           secret)
//       }
//     }
//   }

//   def ressetPasswordTotp(timeout: Duration): Option[TOTP] = {
//     resetPasswordCode.flatMap { secret =>
//       verificationInitialTime.map { initialTime =>
//         TOTP(
//           OTPAlgorithm.SHA1,
//           6,
//           timeout.getSeconds.toInt,
//           initialTime.getEpochSecond,
//           secret)
//       }
//     }
//   }
// }

// object Security {

//   val HASH_LENGTH = 32

//   val tupled = (apply: (
//     HashedCredential,
//     Option[Instant],
//     Option[OTPKey],
//     Option[String],
//     Option[OTPKey],
//     Option[Instant]) => Security).tupled
//   val hasher = HashedCredentialFactory(
//     1, "PBKDF2WithHmacSHA512", "NativePRNGNonBlocking", HASH_LENGTH, HASH_LENGTH, 20000)

//   def apply(password: String)
//     (implicit ec: ExecutionContext): Future[Security] = {
//     hasher
//       .generate(password)
//       .map(apply(_, None, None, None, None, None))
//   }

//   def totp(
//       secret: OTPKey,
//       initialTime: Instant,
//       timeout: Duration = Duration.ofMinutes(15)) = {
//     TOTP(OTPAlgorithm.SHA1, 6, timeout.getSeconds.toInt, initialTime.getEpochSecond, secret)
//   }
// }
