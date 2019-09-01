package config

import java.util.UUID

import _root_.controllers.AssetsComponents
import articles.ArticleComponents
import authentication.AuthenticationComponents
import com.softwaremill.macwire.wire
import commons.CommonsComponents
import play.api.ApplicationLoader.Context
import play.api._
import play.api.cache.AsyncCacheApi
import play.api.cache.ehcache.EhCacheComponents
import play.api.db.evolutions.{ DynamicEvolutions, EvolutionsComponents }
import play.api.db.slick._
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.i18n._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import play.api.routing.Router
import play.filters.cors.{ CORSConfig, CORSFilter }
import slick.basic.{ BasicProfile, DatabaseConfig }
import users.UserComponents

class RealWorldApplicationLoader extends ApplicationLoader {

  def load(context: Context): Application = {
    startH2(context)
    wire[RealWorldComponents].application
  }

  def startH2(context: Context) = {
    import org.h2.jdbc.JdbcSQLException
    import org.h2.tools.{ Server => H2Server }

    if (context.environment.mode == Mode.Dev) {
      // start h2 console server
      try {
        val server = H2Server.createWebServer("-webAllowOthers", "-browser")
        server.start()
        println(server.getStatus)
      } catch {
        case _: JdbcSQLException => ()
      }
    }
  }
}

abstract class BaseComponent(context: Context) extends BuiltInComponentsFromContext(context)
  with SlickComponents
  with SlickEvolutionsComponents
  with CommonsComponents
  with EhCacheComponents {
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

class RealWorldComponents(context: Context)
  extends BaseComponent(context)
  with SlickComponents
  with SlickEvolutionsComponents
  with AssetsComponents
  with I18nComponents
  with EvolutionsComponents
  with AhcWSComponents
  with AuthenticationComponents
  with UserComponents
  with EhCacheComponents {

  val articleComponents: ArticleComponents = wire[ArticleComponents]
  override lazy val dynamicEvolutions: DynamicEvolutions = new DynamicEvolutions

  def onStart(): Unit = {
    // applicationEvolutions is a val and requires evaluation
    applicationEvolutions
  }

  onStart()

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  protected lazy val routes: PartialFunction[RequestHeader, Handler] = userRoutes.orElse(articleComponents.articleRoutes)

  override lazy val router: Router = Router.from(routes)

  override lazy val defaultCacheApi: AsyncCacheApi = cacheApi(UUID.randomUUID().toString)
}