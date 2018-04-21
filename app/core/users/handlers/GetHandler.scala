package core.users.handlers

import scala.concurrent.{ ExecutionContext, Future }

import commons.services.ActionRunner
import core.authentication.api.AuthenticatedUser
import core.users.models.{ UserDetailsWithToken, UserDetailsWithTokenWrapper }
import core.users.services.UserService
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class GetHandler(actionRunner: ActionRunner, userService: UserService)(
  implicit ec: ExecutionContext)
  extends (AuthenticatedUser => Future[JsValue]) {
  override def apply(user: AuthenticatedUser): Future[JsValue] = {
    val email = user.email
    actionRunner
      .runTransactionally(userService.getUserDetails(email))
      .map(userDetails => UserDetailsWithToken(userDetails, user.token))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
  }
}
