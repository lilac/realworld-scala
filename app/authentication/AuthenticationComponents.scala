package authentication

import authentication.api.{ SecurityUserCreator, SecurityUserProvider, SecurityUserUpdater }
import authentication.pac4j.Pac4jComponents
import authentication.repositories.SecurityUserRepo
import authentication.services.SecurityUserService
import com.softwaremill.macwire.wire
import commons.CommonsComponents

trait AuthenticationComponents extends CommonsComponents
  with Pac4jComponents {

  lazy val securityUserCreator: SecurityUserCreator = wire[SecurityUserService]
  lazy val securityUserProvider: SecurityUserProvider = wire[SecurityUserService]
  lazy val securityUserUpdater: SecurityUserUpdater = wire[SecurityUserService]
  lazy val securityUserRepo: SecurityUserRepo = wire[SecurityUserRepo]
}
