https://github.com/ejisan/kuro-otp

Prepare an OTP secret key (OTPSecretKey) for the user:
val secretFromHex = ejisan.kuro.otp.OTPKey.fromHex("5468617473206D79204B756E67204675")

import ejisan.kuro.otp.OTPKey
import scala.util.Random
val secret = OTPKey(new Random(java.security.SecureRandom.getInstance("NativePRNGBlocking")))

<!-- Random OTP Key -->
val  secret2 = ejisan.kuro.otp.OTPKey()
secret2.random(OTPAlgorithm.SHA)

<!-- Hashing Algorithms -->
<!-- Generate PIN code as toke: -->
scala> val totp = TOTP(OTPAlgorithm.SHA1, 6, 30, secretFromHex)
ejisan.kuro.otp.TOTP = TOTP(KRUGC5DTEBWXSICLOVXGOICGOU======, SHA1, 6, 30, 0)

<!-- Generate PIN code with time-step window: -->
TOTP(OTPAlgorithm.SHA1, 6, 30, 5, secretFromHex)
ejisan.kuro.otp.TOTP = TOTP(KRUGC5DTEBWXSICLOVXGOICGOU======, SHA1, 6, 30, 5)


res117.validate(res117.currentTime, res117.otpkey)

<!-- PIN code validation -->
// generate TOTP
code = topt.generate

// validate totp using time and Code
// retuns Boolean..
topt.validate(topt.currentTime, code)