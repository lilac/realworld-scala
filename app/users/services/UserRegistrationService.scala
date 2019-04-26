package users.services

import authentication.api.{ SecurityUserCreator, SecurityUserProvider }
import commons.exceptions.ValidationException
import commons.repositories.DateTimeProvider
import commons.utils.DbioUtils
import authentication.models.{ NewSecurityUser, PlainTextPassword, SecurityUserId }
import users.models.{ User, UserId, UserRegistration }
import users.repositories.UserRepo
import play.api.Configuration
import slick.dbio.{ DBIO, DBIOAction, Effect, NoStream }
import scala.concurrent.ExecutionContext.Implicits.global

import commons.models.Username

private[users] class UserRegistrationService(userRegistrationValidator: UserRegistrationValidator,
                                             securityUserCreator: SecurityUserCreator,
                                             securityUserProvider: SecurityUserProvider,
                                             dateTimeProvider: DateTimeProvider,
                                             userRepo: UserRepo,
                                             config: Configuration) {

  private val defaultImage = Some(config.get[String]("app.defaultImage"))

  def register(userRegistration: UserRegistration): DBIO[(User, SecurityUserId)] = {
    for {
      _ <- validate(userRegistration)
      userAndSecurityUserId <- doRegister(userRegistration)
    } yield userAndSecurityUserId
  }

  private def validate(userRegistration: UserRegistration) = {
    userRegistrationValidator.validate(userRegistration)
      .flatMap(violations => DbioUtils.fail(violations.isEmpty, new ValidationException(violations)))
  }

  private def doRegister(userRegistration: UserRegistration) = {
    val newSecurityUser = NewSecurityUser(userRegistration.email, userRegistration.password)
    for {
      securityUser <- securityUserCreator.create(newSecurityUser)
      now = dateTimeProvider.now
      user = User(UserId(-1), userRegistration.username, userRegistration.email, null, defaultImage, now, now)
      savedUser <- userRepo.insertAndGet(user)
    } yield (savedUser, securityUser.id)
  }

  def bindOrCreate(user: User): DBIO[(User, SecurityUserId)] = {
    for {
      result <- userRepo.findByUsernameOption(user.username)
      newUser <- result match {
        case Some(u) =>
          for {
            su <- securityUserProvider.findByEmail(user.email)
          } yield (u, su.id)
        case None =>
          createUser(user)
      }
    } yield newUser
  }

  def createUser(user: User): DBIO[(User, SecurityUserId)] = {
    for {
      u <- userRepo.insertAndGet(user)
      su <- securityUserCreator.create(NewSecurityUser(user.email, PlainTextPassword("oauth")))
    } yield {
      (u, su.id)
    }
  }
}



