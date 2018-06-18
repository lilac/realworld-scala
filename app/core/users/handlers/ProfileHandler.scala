package core.users.handlers

import scala.concurrent.{ ExecutionContext, Future }

import commons.models.Username
import commons.services.ActionRunner
import core.authentication.api.AuthenticatedUser
import core.users.models.ProfileWrapper
import core.users.services.ProfileService
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
class ProfileHandler(actionRunner: ActionRunner, profileService: ProfileService)(
  implicit ec: ExecutionContext)
  extends Handler2[Option[AuthenticatedUser], Username, JsValue] {
  override def apply(optUser: Option[AuthenticatedUser],
                     username: Username): Future[JsValue] = {
    require(username != null)

    val optEmail = optUser.map(_.email)
    actionRunner
      .runTransactionally(profileService.findByUsername(username, optEmail))
      .map(ProfileWrapper(_))
      .map(Json.toJson(_))
  }
}
