package core.users

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.PathMatcher._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives._
import authentication.AuthModule
import commons.utils.PlayJsonSupport
import core.authentication.api.{ AuthenticatedUser, CredentialsWrapper }
import core.users.models.{ UpdateUserWrapper, UserRegistrationWrapper }
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class Router(val configuration: Configuration)(
  implicit val executionContext: ExecutionContext)
  extends PlayJsonSupport {
  lazy val authModule: AuthModule = new AuthModule(configuration)
  lazy val userModule: UserModule = new UserModule(configuration, authModule)

  import akka.http.scaladsl.server.RouteConcatenation.concat
  import akka.http.scaladsl.server.directives.MarshallingDirectives._
  import akka.http.scaladsl.server.directives.PathDirectives.path
  import akka.http.scaladsl.server.directives.RouteDirectives.complete
  import authModule.authMiddleware.requireUser

  val getRoute: AuthRoute = { user: AuthenticatedUser =>
    ctx =>
      ctx.complete(userModule.getHandler(user))
  }

  val loginRoute: Route =
    entity(as[CredentialsWrapper])(body =>
      ctx => ctx.complete(userModule.loginHandler.login(body)))

  val routes: Route = {
    concat(
      (path("users") & post) {
        entity(as[UserRegistrationWrapper])(body =>
          complete(userModule.registerHandler(body.user)))
      },
      (path("user") & put) (requireUser(updateRoute)),
      (path("user") & get) (requireUser(getRoute)),
      (path("users" / "login") & post) (loginRoute)
    )
  }

  type AuthRoute = AuthenticatedUser => Route
  lazy val updateRoute: AuthRoute = { user: AuthenticatedUser =>
    entity(as[UpdateUserWrapper])(body =>
      complete(userModule.updateHandler(user, body.user)))
  }
}
