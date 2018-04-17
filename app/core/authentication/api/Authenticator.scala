package core.authentication.api

import slick.dbio.DBIO

trait Authenticator[T] {
  def authenticate(request: T): DBIO[String]
}