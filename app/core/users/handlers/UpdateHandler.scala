package core.users.handlers

import scala.concurrent.{ ExecutionContext, Future }

import commons.services.ActionRunner
import core.authentication.api._
import core.commons.HandlerUtil.handleFailedValidation
import core.users.models.{ UserDetailsWithToken, UserDetailsWithTokenWrapper, UserUpdate }
import core.users.services.UserService
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class UpdateHandler(
                     actionRunner: ActionRunner,
                     userService: UserService,
                     securityUserProvider: SecurityUserProvider)(implicit ec: ExecutionContext)
  extends ((AuthenticatedUser, UserUpdate) => Future[JsValue]) {
  override def apply(user: AuthenticatedUser,
                     value: UserUpdate): Future[JsValue] = {
    val email = user.email
    val ops = userService.update(email, value)
    actionRunner
      .run(ops)
      .map(userDetails => UserDetailsWithToken(userDetails, user.token))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .recover(handleFailedValidation)
  }
}
