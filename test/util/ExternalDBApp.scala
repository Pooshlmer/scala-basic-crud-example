package util

import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.execute._

import play.api.db.DB
import play.api.Play.current
import play.api.test._
import play.api.test.Helpers._
import anorm._
import anorm.SqlParser._

// This is one way to specify a test database
// The around function runs every time you use this
abstract class ExternalDBApp (override val app: FakeApplication = 
  FakeApplication(additionalConfiguration = Map(
     "db.default.driver" -> "org.postgresql.Driver",
     "db.default.url" -> "jdbc:postgresql://localhost:5432/calendar_test",
     "db.default.user" -> "testapp",
     "db.default.password" -> "password"  
  ))) extends WithApplication {
  
  override def around[T: AsResult](t: => T): Result = super.around {
    setupData()
    t
  }
  
  // This happens before every test using this class
  def setupData() = {
  }
}