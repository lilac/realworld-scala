package users.services

import java.time.Instant

import scala.concurrent.{ ExecutionContext, Future }

import akka.stream.Materializer
import authentication.models.{ JwtToken, PlainTextPassword, SecurityUserId }
import commons.models.{ Email, Username }
import commons.services.ActionRunner
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import users.models.{ OAuthCode, User, UserDetailsWithToken, UserId, UserRegistration }

/**
 * Copyright SameMo 2019
 */
class GithubService(ws: WSClient,
                    config: Configuration,
                    actionRunner: ActionRunner,
                    userRegistrationService: UserRegistrationService)(implicit ec: ExecutionContext) {
  private val githubClientId =
    config.getOptional[String]("oauth.github.clientId").getOrElse("")
  private val githubClientSecret =
    config.getOptional[String]("oauth.github.clientSecret").getOrElse("")

  def authenticate(code: OAuthCode): Future[(User, SecurityUserId)] = {
    for {
      response <- ws
        .url("https://github.com/login/oauth/access_token")
        .withHttpHeaders("Accept" -> "application/json")
        .post(
          Map("client_id" -> githubClientId,
            "client_secret" -> githubClientSecret,
            "code" -> code.code,
            "state" -> "play"))
      token = response.json("access_token").as[String]
      profile <- ws
        .url(s"https://api.github.com/user?access_token=$token")
        .withHttpHeaders("Accept" -> "application/json")
        .get()
      js = profile.json
      user <- actionRunner.runTransactionally(bindUser(js))
    } yield user
  }

  def bindUser(js: JsValue) = {
    val login = js("login").as[String]
    val email = js("email").asOpt[String].getOrElse(s"$login@github")
    //       val name = js("name").asOpt[String].getOrElse(login)
    val headImg = js("avatar_url").asOpt[String]
    //       val location = js("location").asOpt[String].getOrElse("")
    val bio = js("bio").asOpt[String]
    //       val githubUrl = js("html_url").as[String]
    val now = Instant.now
    val user = User(UserId(-1), Username(login), Email(email), bio, headImg, now, now)
    userRegistrationService.bindOrCreate(user)
  }

}
