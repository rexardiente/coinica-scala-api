package utils

class SchemaGenerationModule extends com.google.inject.AbstractModule {
  override protected def configure() = {
    bind(classOf[utils.db.schema.Generator]).asEagerSingleton()
    bind(classOf[utils.db.schema.DBDefaultGenerator]).asEagerSingleton()
  }
}
