package core.users

import scala.concurrent.ExecutionContext

import authentication.AuthModule
import commons.CommonsModule
import core.users.handlers.{ GetHandler, LoginHandler, RegisterHandler, UpdateHandler }
import core.users.repositories.UserRepo
import core.users.services._
import play.api.Configuration

/**
 * Copyright SameMo 2018
 */
class UserModule(configuration: Configuration, authModule: AuthModule)(
  implicit ec: ExecutionContext) {

  import CommonsModule.actionRunner
  import authModule.securityUserProvider

  // handlers
  lazy val registerHandler: RegisterHandler = new RegisterHandler(
    actionRunner,
    userRegistrationService,
    authModule.jwtGenerator)
  lazy val updateHandler = new UpdateHandler(actionRunner, userService, authModule.securityUserProvider)
  lazy val loginHandler = new LoginHandler(actionRunner, authModule.passAuthenticator, userService)
  lazy val getHandler = new GetHandler(actionRunner, userService)
  // repo
  lazy val userRepo: UserRepo = new UserRepo

  // services
  lazy val userRegistrationService: UserRegistrationService =
    new UserRegistrationService(userRegistrationValidator,
      authModule.securityUserCreator,
      CommonsModule.dateTimeProvider,
      userRepo,
      configuration)
  lazy val userService = new UserService(userRepo,
    securityUserProvider,
    authModule.securityUserUpdater,
    CommonsModule.dateTimeProvider,
    userUpdateValidator,
    ec)
  // validators
  lazy val userRegistrationValidator: UserRegistrationValidator =
    new UserRegistrationValidator(passwordValidator,
      usernameValidator,
      emailValidator,
      actionRunner,
      ec)
  lazy val passwordValidator: PasswordValidator = new PasswordValidator
  lazy val emailValidator: EmailValidator =
    new EmailValidator(securityUserProvider, ec)
  lazy val usernameValidator: UsernameValidator =
    new UsernameValidator(userRepo, ec)
  lazy val userUpdateValidator: UserUpdateValidator = new UserUpdateValidator(
    usernameValidator,
    emailValidator,
    passwordValidator,
    ec)
}
