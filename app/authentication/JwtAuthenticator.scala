package authentication

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import authentikat.jwt.{ JsonWebToken, JwtClaimsSet, JwtHeader }
import core.authentication.api.SecurityUserId
import org.json4s.JValue
import org.json4s.JsonAST.{ JInt, JNothing }

/**
 * Copyright SameMo 2018
 */
class JwtAuthenticator(secret: String) {

  import JwtAuthenticator._

  def authenticated: Directive1[AuthInfo] =
    optionalHeaderValueByName("Authorization").flatMap { value =>
      val tokenRegex = """Token (.*)""".r
      val tok = value map {
        case tokenRegex(t) => t
        case _ => ""
      }
      tok match {
        case Some(jwt) if isTokenExpired(jwt) =>
          complete(StatusCodes.Unauthorized -> "Token expired.")

        case Some(jwt) if JsonWebToken.validate(jwt, secret) =>
          val claims = getClaims(jwt).getOrElse(JNothing)
          provide((jwt, claims))

        case _ => complete(StatusCodes.Unauthorized)
      }
    }

  def generateToken(id: SecurityUserId): String = {
    val header = JwtHeader("HS256")
    JsonWebToken(header, claims(id.value, 1), secret)
  }
}

object JwtAuthenticator {
  private def claims(id: Long, expiryPeriodInDays: Long) =
    JwtClaimsSet(
      Map("sub" -> id,
        "exp" -> (System.currentTimeMillis() + TimeUnit.DAYS
          .toMillis(expiryPeriodInDays)) / 1000)
    )

  private def getClaims(jwt: String): Option[JValue] = jwt match {
    case JsonWebToken(_, claims, _) => Some(claims.jvalue)
    case _ => None
  }

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims \ "exp" match {
        case JInt(value) => value.toLong * 1000 < System.currentTimeMillis()
        case _ => false
      }
    case None => false
  }

  type AuthInfo = (String, JValue)
}
