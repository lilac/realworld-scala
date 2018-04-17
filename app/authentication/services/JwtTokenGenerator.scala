package authentication.services

import java.time.Duration
import java.util.concurrent.TimeUnit

import authentikat.jwt.{ JsonWebToken, JwtClaimsSet, JwtHeader }
import commons.CommonsModule
import core.authentication.api.{ JwtToken, SecurityUserId, SecurityUserIdProfile, TokenGenerator }

/**
 * Copyright SameMo 2018
 */
class JwtTokenGenerator(secret: String) extends TokenGenerator[SecurityUserIdProfile, JwtToken] {
  private val tokenDuration = Duration.ofHours(12)

  override def generate(profile: SecurityUserIdProfile): JwtToken = {
    val expiredAt = CommonsModule.dateTimeProvider.now.plus(tokenDuration)

    val token = generateToken(profile.securityUserId)
    JwtToken(token, expiredAt)
  }

  def generateToken(id: SecurityUserId): String = {
    val header = JwtHeader("HS256")
    JsonWebToken(header, claims(id.value, 1), secret)
  }

  private def claims(id: Long, expiryPeriodInDays: Long) =
    JwtClaimsSet(
      Map("sub" -> id,
        "exp" -> (System.currentTimeMillis() + TimeUnit.DAYS
          .toMillis(expiryPeriodInDays)) / 1000)
    )
}
