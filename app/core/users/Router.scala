package core.users

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.PathMatcher._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.directives.ExecutionDirectives.handleExceptions
import akka.http.scaladsl.server.directives.MethodDirectives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import authentication.AuthModule
import commons.models.Username
import commons.utils.PlayJsonSupport
import core.authentication.api.{ AuthenticatedUser, CredentialsWrapper }
import core.users.exceptions.MissingUserException
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
  import authModule.authMiddleware.{ optionalUser, requireUser }

  val getRoute: AuthRoute = { user: AuthenticatedUser =>
    ctx =>
      ctx.complete(userModule.getHandler(user))
  }

  val loginRoute: Route =
    entity(as[CredentialsWrapper])(body =>
      ctx => ctx.complete(userModule.loginHandler.login(body)))

  def followRoute(s: String, user: AuthenticatedUser): Route = { ctx =>
    val username = Username(s)
    ctx.complete(userModule.followHandler(user, username))
  }

  def unfollowRoute(s: String, user: AuthenticatedUser): Route = { ctx =>
    val username = Username(s)
    ctx.complete(userModule.unfollowHandler(user, username))
  }

  def getProfileRoute(s: String, user: Option[AuthenticatedUser]): Route = { ctx =>
    val username = Username(s)
    ctx.complete(userModule.profileHandler(user, username))
  }

  val routes: Route = {
    concat(
      (path("users") & post) {
        entity(as[UserRegistrationWrapper])(body =>
          complete(userModule.registerHandler(body.user)))
      },
      (path("users" / "login") & post) (loginRoute),
      (path("user") & put & requireUser) (updateRoute),
      (path("user") & get & requireUser) (getRoute),
      profileRoute
    )
  }

  lazy val profileRoute: Route = handleExceptions(exceptionHandler)(
    concat(
      (path("profiles" / Segment / "follow") & post & requireUser) (followRoute),
      (path("profiles" / Segment / "follow") & delete & requireUser) (unfollowRoute),
      (path("profiles" / Segment) & get & optionalUser) (getProfileRoute)
    )
  )

  type AuthRoute = AuthenticatedUser => Route
  lazy val updateRoute: AuthRoute = { user: AuthenticatedUser =>
    entity(as[UpdateUserWrapper])(body =>
      complete(userModule.updateHandler(user, body.user)))
  }

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: MissingUserException => ctx =>
        ctx.complete(StatusCodes.NotFound)
    }
}
