package core.users

import scala.concurrent.ExecutionContext

import authentication.AuthModule
import commons.CommonsModule
import core.users.repositories.UserRepo
import core.users.services._
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class UserModule(configuration: Configuration, authModule: AuthModule)(implicit ec: ExecutionContext) {

  import CommonsModule.actionRunner
  import authModule.securityUserProvider

  lazy val userHandlers: UserHandlers = new UserHandlers(CommonsModule.actionRunner, userRegistrationService, authModule.pack4jJwtAuthenticator)
  //  lazy val userService: UserService = wire[UserService]
  lazy val userRepo: UserRepo = new UserRepo
  lazy val userRegistrationService: UserRegistrationService = new UserRegistrationService(
    userRegistrationValidator,
    authModule.securityUserCreator, CommonsModule.dateTimeProvider, userRepo, configuration)
  lazy val userRegistrationValidator: UserRegistrationValidator = new UserRegistrationValidator(passwordValidator, usernameValidator, emailValidator, actionRunner, ec)
  lazy val passwordValidator: PasswordValidator = new PasswordValidator
  lazy val emailValidator: EmailValidator = new EmailValidator(securityUserProvider, ec)
  lazy val usernameValidator: UsernameValidator = new UsernameValidator(userRepo, ec)
}
