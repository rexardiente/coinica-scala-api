package models.domain.enum

object VIP extends Enumeration {
  type value = Value

  val Bronze = Value("BRONZE")
  val Silver = Value("SILVER")
  val Gold   = Value("GOLD")
}

object VIPBenefitAmount extends Enumeration {
  type value = Value

  val Bronze = Value(1000)
  val Silver = Value(10000)
  val Gold 	 = Value(100000)
}

object VIPBenefitPoints extends Enumeration {
  type value = Value

  val Bronze = Value(100)
  val Silver = Value(1000)
  val Gold 	 = Value(5000)
}
