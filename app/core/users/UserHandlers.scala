package core.users

import scala.concurrent.{ ExecutionContext, Future }

import authentication.JwtAuthenticator
import commons.services.ActionRunner
import core.commons.HandlerUtil.handleFailedValidation
import core.users.models._
import core.users.services.UserRegistrationService
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class UserHandlers(actionRunner: ActionRunner,
                   userRegistrationService: UserRegistrationService,
                   authenticator: JwtAuthenticator) {

  def register(user: UserRegistration)(implicit ec: ExecutionContext): Future[JsValue] = {
    actionRunner
      .runTransactionally(userRegistrationService.register(user))
      .map(userAndSecurityUserId => {
        val (user, securityUserId) = userAndSecurityUserId
        val jwtToken = authenticator.generateToken(securityUserId)
        UserDetailsWithToken(user.email,
          user.username,
          user.createdAt,
          user.updatedAt,
          user.bio,
          user.image,
          jwtToken)
      })
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .recover(handleFailedValidation)
  }
}
