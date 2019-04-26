package users.models

/**
 * Copyright SameMo 2019
 */

case class OAuthCode(code: String, state: String)

object OAuthCode {

  import play.api.libs.json.{ Format, Json }

  implicit val format: Format[OAuthCode] = Json.format[OAuthCode]
}
