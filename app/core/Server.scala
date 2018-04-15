package core

import scala.concurrent.duration.{ Duration, _ }
import scala.concurrent.{ Await, ExecutionContext }

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{ concat, pathPrefix }
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.{ Config, ConfigFactory }
import core.users.UserRouter
import org.flywaydb.core.Flyway
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
object Server {
  implicit val system: ActorSystem = ActorSystem("Web")

  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val userRouter: UserRouter = new UserRouter(configuration)
  lazy val flyway = new Flyway()

  val config: Config = ConfigFactory.load()
  val dbConfig: Config = config.getConfig("slick.dbs.default.db")

  def configuration: Configuration = {
    Configuration(config)
  }

  def migrate(): Int = {
    val user = dbConfig.getString("user")
    val url = dbConfig.getString("url")
    val pass = dbConfig.getString("password")
    flyway.setDataSource(url, user, pass)
    flyway.migrate()
  }

  def main(args: Array[String]): Unit = {
    val port =
      if (args.isEmpty) 9000
      else args.head.toInt

    migrate()

    val route: Route = concat(
      //        cors()(
      pathPrefix("api")(
        concat(
          userRouter.routes
        )
      )
    )

    Await.result(Http().bindAndHandle(route, "0.0.0.0", port), 1.seconds)
    println(s"Server started (port: $port)")

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
