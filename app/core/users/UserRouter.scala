package core.users

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives._
import authentication.AuthModule
import commons.utils.PlayJsonSupport
import core.users.models.UserRegistrationWrapper
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class UserRouter(val configuration: Configuration)(implicit val executionContext: ExecutionContext) extends PlayJsonSupport {
  lazy val authModule: AuthModule = new AuthModule(configuration)
  lazy val userModule: UserModule = new UserModule(configuration, authModule)

  val routes: Route = {
    import akka.http.scaladsl.server.RouteConcatenation.concat
    import akka.http.scaladsl.server.directives.MarshallingDirectives._
    import akka.http.scaladsl.server.directives.PathDirectives.path
    import akka.http.scaladsl.server.directives.RouteDirectives.complete

    concat(
      (path("users") & post) {
        entity(as[UserRegistrationWrapper])(body =>
          complete(userModule.userHandlers.register(body.user)))
      }
    )
  }
}
