package config

import java.util.UUID

import _root_.controllers.AssetsComponents
import articles.ArticleComponents
import authentication.AuthenticationComponents
import com.softwaremill.macwire.wire
import play.api.ApplicationLoader.Context
import play.api._
import play.api.cache.AsyncCacheApi
import play.api.cache.ehcache.EhCacheComponents
import play.api.db.evolutions.{ DynamicEvolutions, EvolutionsComponents }
import play.api.i18n._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
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

class RealWorldComponents(context: Context)
  extends BaseComponent(context)
  with AssetsComponents
  with I18nComponents
  with EvolutionsComponents
  with AhcWSComponents
  with AuthenticationComponents
  with EhCacheComponents {

  protected val userComponents: UserComponents = wire[UserComponents]
  protected val articleComponents: ArticleComponents = wire[ArticleComponents]
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

  override lazy val routes: PartialFunction[RequestHeader, Handler] = userComponents.routes
    .orElse(articleComponents.routes)

  override lazy val defaultCacheApi: AsyncCacheApi = cacheApi(UUID.randomUUID().toString)
}