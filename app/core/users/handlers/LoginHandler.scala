package core.users.handlers

import scala.concurrent.{ ExecutionContext, Future }

import commons.services.ActionRunner
import core.authentication.api.{ Authenticator, CredentialsWrapper, InvalidPasswordException, MissingSecurityUserException }
import core.users.models.{ UserDetailsWithToken, UserDetailsWithTokenWrapper }
import core.users.services.UserService
import play.api.libs.json.{ JsObject, JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class LoginHandler(actionRunner: ActionRunner,
                   authenticator: Authenticator[CredentialsWrapper],
                   userService: UserService)(implicit ec: ExecutionContext) {

  def login(cred: CredentialsWrapper): Future[JsValue] = {
    val loginAction = authenticator
      .authenticate(cred)
      .zip(userService.getUserDetails(cred.user.email))
      .map(tokenAndUserDetails =>
        UserDetailsWithToken(tokenAndUserDetails._2, tokenAndUserDetails._1))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))

    actionRunner
      .runTransactionally(loginAction)
      .recover({
        case _: InvalidPasswordException | _: MissingSecurityUserException =>
          val violation =
            JsObject(Map("email or password" -> Json.toJson(Seq("is invalid"))))
          val response = JsObject(Map("errors" -> violation))
          Json.toJson(response)
      })
  }
}
