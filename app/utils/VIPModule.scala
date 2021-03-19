package utils

class VIPModule extends com.google.inject.AbstractModule with play.api.libs.concurrent.AkkaGuiceSupport {
  override protected def configure() = {
  	bindActor[utils.db.schema.VIPBenefitGenerator]("VIPBenefitGenerator")
  }
}
