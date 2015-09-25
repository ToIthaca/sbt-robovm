package org.jboss.shrinkwrap.resolver.api

import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem

object MavenResolver {
  def apply() = ResolverSystemFactory
    .createFromUserView(
      classOf[ConfigurableMavenResolverSystem],
      //Class which the resolver breaks on
      Class.forName("org.jboss.shrinkwrap.resolver.spi.loader.SpiServiceLoader").getClassLoader
    )
}
