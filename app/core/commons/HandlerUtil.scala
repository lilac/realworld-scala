package core.commons

import commons.exceptions.ValidationException
import core.commons.models.ValidationResultWrapper
import play.api.libs.json.{ JsValue, Json }

/**
 * Copyright SameMo 2018
 */
object HandlerUtil {

  def handleFailedValidation: PartialFunction[Throwable, JsValue] = {
    case e: ValidationException =>
      val errors = e.violations
        .groupBy(_.property)
        .mapValues(_.map(propertyViolation => propertyViolation.violation.message))

      val wrapper: ValidationResultWrapper = ValidationResultWrapper(errors)
      Json.toJson(wrapper)
  }
}
