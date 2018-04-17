package core.users

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.PathMatcher._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives._
import authentication.AuthModule
import commons.utils.PlayJsonSupport
import core.authentication.api.CredentialsWrapper
import core.users.models.{ UpdateUserWrapper, UserRegistrationWrapper }
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class UserRouter(val configuration: Configuration)(
  implicit val executionContext: ExecutionContext)
  extends PlayJsonSupport {
  lazy val authModule: AuthModule = new AuthModule(configuration)
  lazy val userModule: UserModule = new UserModule(configuration, authModule)

  import akka.http.scaladsl.server.RouteConcatenation.concat
  import akka.http.scaladsl.server.directives.MarshallingDirectives._
  import akka.http.scaladsl.server.directives.PathDirectives.path
  import akka.http.scaladsl.server.directives.RouteDirectives.complete
  import authModule.authenticator.authenticated

  val loginRoute: Route =
    entity(as[CredentialsWrapper])(body =>
      ctx => ctx.complete(userModule.loginHandler.login(body)))

  val routes: Route = {
    concat(
      (path("users") & post) {
        entity(as[UserRegistrationWrapper])(body =>
          complete(userModule.userHandlers.register(body.user)))
      },
      /*authenticated { authInfo =>
        (path("user") & put) (updateRoute(authInfo))
      },*/
      (path("user") & put) (authenticated(updateRoute)),
      (path("users" / "login") & post) (loginRoute)
    )
  }

  import authentication.JwtAuthenticator.AuthInfo

  type AuthRoute = AuthInfo => Route
  lazy val updateRoute: AuthRoute = { authInfo: AuthInfo =>
    entity(as[UpdateUserWrapper])(body =>
      complete(userModule.userUpdateHandler(authInfo, body.user)))
  }
}
