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
class FollowHandler(actionRunner: ActionRunner, profileService: ProfileService)(implicit ec: ExecutionContext)
  extends Handler2[AuthenticatedUser, Username, JsValue] {
  override def apply(user: AuthenticatedUser,
                     username: Username): Future[JsValue] = {
    require(username != null)

    val currentUserEmail = user.email
    actionRunner
      .runTransactionally(profileService.follow(username, currentUserEmail))
      .map(ProfileWrapper(_))
      .map(Json.toJson(_))
  }
}
