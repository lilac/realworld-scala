package core.authentication.api

import commons.models.Email
import slick.dbio.DBIO

trait SecurityUserProvider {
  def findByEmail(email: Email): DBIO[Option[SecurityUser]]

  def findById(id: SecurityUserId): DBIO[Option[SecurityUser]]
}