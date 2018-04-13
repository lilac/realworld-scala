package commons.repositories

import slick.basic.DatabaseConfig
import slick.jdbc.{ JdbcBackend, JdbcProfile }

class DbConfigHelper {
  private val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default")

  val db: JdbcBackend#DatabaseDef = dbConfig.db

  val driver: JdbcProfile = dbConfig.profile
}
