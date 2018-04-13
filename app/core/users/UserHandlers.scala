package core.users

import scala.concurrent.{ ExecutionContext, Future }

import commons.services.ActionRunner
import core.authentication.api.{ JwtToken, SecurityUserId, SecurityUserIdProfile, TokenGenerator }
import core.commons.HandlerUtil
import core.users.models.{ UserDetailsWithToken, UserDetailsWithTokenWrapper, UserRegistration }
import core.users.services.UserRegistrationService
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class UserHandlers(actionRunner: ActionRunner,
                   userRegistrationService: UserRegistrationService,
                   jwtAuthenticator: TokenGenerator[SecurityUserIdProfile, JwtToken]) {

  def register(user: UserRegistration)(implicit ec: ExecutionContext): Future[JsValue] = {
    actionRunner
      .runTransactionally(userRegistrationService.register(user))
      .map(userAndSecurityUserId => {
        val (user, securityUserId) = userAndSecurityUserId
        val jwtToken: JwtToken = generateToken(securityUserId)
        UserDetailsWithToken(user.email,
          user.username,
          user.createdAt,
          user.updatedAt,
          user.bio,
          user.image,
          jwtToken.token)
      })
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .recover(HandlerUtil.handleFailedValidation)
  }

  private def generateToken(securityUserId: SecurityUserId) = {
    val profile = SecurityUserIdProfile(securityUserId)
    val jwtToken = jwtAuthenticator.generate(profile)
    jwtToken
  }
}
