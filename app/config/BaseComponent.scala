package config

import commons.CommonsComponents
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.cache.ehcache.EhCacheComponents
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.db.slick._
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.routing.Router.Routes
import play.filters.cors.{ CORSConfig, CORSFilter }
import slick.basic.{ BasicProfile, DatabaseConfig }

/**
 * Copyright SameMo 2019
 */
abstract class BaseComponent(context: Context) extends BuiltInComponentsFromContext(context)
  with CommonsComponents
  with EhCacheComponents
  with SlickEvolutionsComponents {

  val routes: Routes

  override lazy val router: Router = Router.from(routes)

  override lazy val slickApi: SlickApi =
    new DefaultSlickApi(environment, configuration, applicationLifecycle)(executionContext)

  override lazy val databaseConfigProvider: DatabaseConfigProvider = new DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = slickApi.dbConfig[P](DbName("default"))
  }

  private lazy val corsFilter: CORSFilter = {
    val corsConfig = CORSConfig.fromConfiguration(configuration)

    CORSFilter(corsConfig)
  }

  override def httpFilters: Seq[EssentialFilter] = List(corsFilter)
}
