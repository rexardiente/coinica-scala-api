package models.domain.enum

object VIP extends Enumeration {
  type value = Value

  val BRONZE = Value
  val SILVER = Value
  val GOLD   = Value
}

object VIPBenefitAmount extends Enumeration {
  type value = Value

  val BRONZE = Value(1000)
  val SILVER = Value(10000)
  val GOLD 	 = Value(100000)
}

object VIPBenefitPoints extends Enumeration {
  type value = Value

  val BRONZE = Value(100)
  val SILVER = Value(1000)
  val GOLD 	 = Value(5000)
}
