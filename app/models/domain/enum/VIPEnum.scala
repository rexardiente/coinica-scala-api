package models.domain.enum

object VIP extends Enumeration {
  type value = Value

  val BRONZE = Value
  val SILVER = Value
  val GOLD   = Value
}

object VIPBenefitAmount extends Enumeration {
  type value = Value

  var BRONZE = Value(1000)
  var SILVER = Value(10000)
  var GOLD 	 = Value(100000)
}

object VIPBenefitPoints extends Enumeration {
  type value = Value

  var BRONZE = Value(50)
  var SILVER = Value(200)
  var GOLD 	 = Value(1000)
}
