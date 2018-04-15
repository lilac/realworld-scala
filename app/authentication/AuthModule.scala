package authentication

import authentication.repositories.SecurityUserRepo
import authentication.services.SecurityUserService
import commons.CommonsModule
import commons.CommonsModule.dateTimeProvider
import core.authentication.api._
import org.pac4j.core.profile.CommonProfile
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class AuthModule(configuration: Configuration) {
  private val secret: String = configuration.get[String]("play.http.secret.key")

  private lazy val signatureConfig = new SecretSignatureConfiguration(secret)
  protected lazy val jwtGenerator: JwtGenerator[CommonProfile] = new JwtGenerator(signatureConfig)
  lazy val authenticator = new JwtAuthenticator(secret)

  lazy val securityUserService = new SecurityUserService(securityUserRepo, dateTimeProvider, CommonsModule.actionRunner)
  lazy val securityUserCreator: SecurityUserCreator = securityUserService
  lazy val securityUserProvider: SecurityUserProvider = securityUserService
  lazy val securityUserUpdater: SecurityUserUpdater = securityUserService
  lazy val securityUserRepo: SecurityUserRepo = new SecurityUserRepo
}
