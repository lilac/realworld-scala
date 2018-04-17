package authentication.pac4j.services

import scala.concurrent.ExecutionContext

import authentication.repositories.SecurityUserRepo
import commons.services.ActionRunner
import commons.utils.DbioUtils.optionToDbio
import core.authentication.api.{ MissingSecurityUserException, _ }
import org.mindrot.jbcrypt.BCrypt
import slick.dbio.DBIO

private[authentication] class UsernameAndPasswordAuthenticator(tokenGenerator: TokenGenerator[SecurityUserIdProfile, JwtToken],
                                                               actionRunner: ActionRunner,
                                                               securityUserRepo: SecurityUserRepo)
                                                              (implicit private val ec: ExecutionContext)
  extends Authenticator[CredentialsWrapper] {

  override def authenticate(value: CredentialsWrapper): DBIO[String] = {
    val credentials = value.user

    val rawEmail = credentials.email.value
    securityUserRepo.findByEmail(credentials.email)
      .flatMap(optionToDbio(_, new MissingSecurityUserException(rawEmail)))
      .map(user => {
        if (authenticated(credentials.password, user)) tokenGenerator.generate(SecurityUserIdProfile(user.id)).token
        else throw new InvalidPasswordException(rawEmail)
      })
  }

  private def authenticated(givenPassword: PlainTextPassword, secUsr: SecurityUser) = {
    BCrypt.checkpw(givenPassword.value, secUsr.password.value)
  }

}