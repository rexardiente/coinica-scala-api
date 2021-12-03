package utils

import utils.db.schema.{ Generator, DBDefaultGenerator }

class SchemaGenerationModule extends com.google.inject.AbstractModule with play.api.libs.concurrent.AkkaGuiceSupport {
  override protected def configure() = {
    bind(classOf[Generator]).asEagerSingleton()
    bindActor[DBDefaultGenerator]("DBDefaultGenerator")
  }
}
