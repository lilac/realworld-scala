package commons.utils

import commons.validations.constraints.Violation

/**
 * Copyright SameMo 2018
 */
trait Validator[T] extends (T => Seq[Violation])