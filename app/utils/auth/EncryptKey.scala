package utils.auth

import java.security.MessageDigest

object Hex {
  private val toDigits: Array[Char] = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  def encodeHexString(data: Array[Byte]): String = new String(encodeHex(data))
  def encodeHex(data: Array[Byte]): Array[Char] = {
    val l = data.length
    val out = Array.ofDim[Char](l << 1)
    // two characters form the hex value
    var j = 0
    0 until l foreach { i =>
      out(j) = toDigits((0xF0 & data(i)) >>> 4) // '>>>' means right shift with zero-padding
      j += 1
      out(j) = toDigits(0x0F & data(i))
      j += 1
    }
    out
  }
}

class EncryptKey {
  def toSHA256(str: String): String = {
    val sha256 = MessageDigest.getInstance("SHA-256")
    Hex.encodeHexString(sha256.digest(str.getBytes("UTF-8")))
  }

  def toMD5(str: String): String = {
    val md5 = MessageDigest.getInstance("MD5")
    Hex.encodeHexString(md5.digest(str.getBytes("UTF-8")))
  }
}