package commons.utils

/**
 * Copyright SameMo 2018
 */
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive1, Rejection }
import commons.validations.constraints.Violation

object ValidationDirective extends PlayJsonSupport {
  final case class ValidationRejection(errors: Seq[Violation]) extends Rejection

  def validate[T](model: T)(implicit validator: Validator[T]): Directive1[T] = {
    validator(model) match {
      case Nil => provide(model)
      case errors: Seq[Violation] => reject(ValidationRejection(errors))
    }
  }
}
