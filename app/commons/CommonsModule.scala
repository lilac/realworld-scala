package commons

import commons.repositories.{ DateTimeProvider, DbConfigHelper }
import commons.services.{ ActionRunner, InstantProvider }

/**
 * Copyright SameMo 2018
 */
object CommonsModule {
  lazy val dbHelper: DbConfigHelper = new DbConfigHelper()
  lazy val actionRunner: ActionRunner = new ActionRunner(dbHelper)
  lazy val dateTimeProvider: DateTimeProvider = new InstantProvider
}
