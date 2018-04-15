package core.users

import scala.concurrent.{ ExecutionContext, Future }

import authentication.JwtAuthenticator.AuthInfo
import commons.services.ActionRunner
import commons.utils.DbioUtils
import core.authentication.api.{ MissingSecurityUserException, SecurityUserId, SecurityUserProvider }
import core.commons.HandlerUtil.handleFailedValidation
import core.users.models.{ UserDetailsWithToken, UserDetailsWithTokenWrapper, UserUpdate }
import core.users.services.UserService
import org.json4s.JInt
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class UserUpdateHandler(
                         actionRunner: ActionRunner,
                         userService: UserService,
                         securityUserProvider: SecurityUserProvider)(implicit ec: ExecutionContext)
  extends ((AuthInfo, UserUpdate) => Future[JsValue]) {
  override def apply(authInfo: AuthInfo, value: UserUpdate): Future[JsValue] = {
    val (token, claims) = authInfo
    val id =
      claims.\("sub") match {
        case JInt(s) => Some(SecurityUserId(s.longValue()))
        case _ => None
      }
    val ops = for {
      id <- DbioUtils.optionToDbio(id, new MissingIdException(authInfo))
      email <- existsSecurityUser(id)
      user <- userService.update(email, value)
    } yield user
    actionRunner
      .runTransactionally(ops)
      .map(userDetails => UserDetailsWithToken(userDetails, token))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .recover(handleFailedValidation)
  }

  private def existsSecurityUser(securityUserId: SecurityUserId) = {
    securityUserProvider
      .findById(securityUserId)
      .flatMap(
        maybeSecurityUser =>
          DbioUtils.optionToDbio(
            maybeSecurityUser,
            new MissingSecurityUserException(s"id:$securityUserId")))
      .map(securityUser => securityUser.email)
  }

  class MissingIdException(auth: AuthInfo)
    extends RuntimeException(auth.toString)

}
