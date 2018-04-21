package core.users

import scala.concurrent.Future

/**
 * Copyright SameMo 2018
 */
package object handlers {
  type M[_] = Future[_]
  type Handler[Req, Res] = Req => M[Res]
  type Handler2[A, T, R] = (A, T) => M[R]
  type Handler3[A, B, C, R] = (A, B, C) => M[R]
}
