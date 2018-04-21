package authentication

import scala.concurrent.ExecutionContext

import authentication.pac4j.services.UsernameAndPasswordAuthenticator
import authentication.repositories.SecurityUserRepo
import authentication.services.{ JwtTokenGenerator, SecurityUserService }
import commons.CommonsModule
import commons.CommonsModule.dateTimeProvider
import core.authentication.api._
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class AuthModule(configuration: Configuration)(implicit ec: ExecutionContext) {
  private val secret: String = configuration.get[String]("play.http.secret.key")

  import CommonsModule.actionRunner

  lazy val jwtGenerator = new JwtTokenGenerator(secret)
  lazy val authenticator = new JwtAuthenticator(secret)
  lazy val passAuthenticator = new UsernameAndPasswordAuthenticator(jwtGenerator, actionRunner, securityUserRepo)
  lazy val userMiddleware = new UserMiddleware(authenticator, securityUserProvider, actionRunner)

  lazy val securityUserService = new SecurityUserService(securityUserRepo, dateTimeProvider, actionRunner)
  lazy val securityUserCreator: SecurityUserCreator = securityUserService
  lazy val securityUserProvider: SecurityUserProvider = securityUserService
  lazy val securityUserUpdater: SecurityUserUpdater = securityUserService
  lazy val securityUserRepo: SecurityUserRepo = new SecurityUserRepo
}
