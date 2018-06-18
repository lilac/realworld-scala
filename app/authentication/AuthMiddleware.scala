package authentication

import scala.concurrent.{ ExecutionContext, Future }

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.ExecutionDirectives.handleExceptions
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.http.scaladsl.server.{ Directive1, ExceptionHandler }
import authentication.JwtAuthenticator.AuthInfo
import commons.services.ActionRunner
import commons.utils.DbioUtils
import core.authentication.api._
import org.json4s.JsonAST.JInt

/**
 * Copyright SameMo 2018
 */
class AuthMiddleware(
                      authenticator: JwtAuthenticator,
                      securityUserProvider: SecurityUserProvider,
                      actionRunner: ActionRunner)(implicit ec: ExecutionContext) {

  def extractUser: Directive1[AuthenticatedUser] =
    authenticatedUser.flatMap { f =>
      FutureDirectives.onSuccess(f)
    }

  def optionalUser: Directive1[Option[AuthenticatedUser]] =
    for {
      f <- authenticatedUser
      d <- FutureDirectives.onComplete(f).map(_.toOption)
    } yield d

  def authenticatedUser: Directive1[Future[AuthenticatedUser]] =
    authenticator.authenticated.map { authInfo =>
      getUser(authInfo).map { user =>
        AuthenticatedUser(user.email, authInfo._1)
      }
    }

  def requireUser: Directive1[AuthenticatedUser] = handleExceptions(exceptionHandler) & extractUser

  def getUser(authInfo: AuthInfo): Future[SecurityUser] = {
    val (_, claims) = authInfo
    val id =
      claims.\("sub") match {
        case JInt(s) => Some(SecurityUserId(s.longValue()))
        case _ => None
      }
    val ops = for {
      id <- DbioUtils.optionToDbio(id, new MissingIdException(authInfo))
      user <- existsSecurityUser(id)
    } yield user
    actionRunner.run(ops)
  }

  private def existsSecurityUser(securityUserId: SecurityUserId) = {
    securityUserProvider
      .findById(securityUserId)
      .flatMap(
        maybeSecurityUser =>
          DbioUtils.optionToDbio(
            maybeSecurityUser,
            new MissingSecurityUserException(s"id:$securityUserId")))
  }

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: MissingIdException => ctx =>
        ctx.complete(StatusCodes.Unauthorized)
      case _: MissingSecurityUserException => ctx =>
        ctx.complete(StatusCodes.Forbidden)
    }

  class MissingIdException(auth: AuthInfo)
    extends RuntimeException(auth.toString)

}
