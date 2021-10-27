package modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names

// A Module is needed to register bindings
class Startup extends AbstractModule {
  override def configure() = {
    bind(classOf[StartUpServices]).asEagerSingleton
  }
}