package util

import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.execute._

import play.api.db.DB
import play.api.test._
import play.api.test.Helpers._

// This is one way to specify a test database
// The around function runs every time you use this
abstract class ExternalDBApp (override val app: FakeApplication = 
  FakeApplication(additionalConfiguration = Map(
     "db.default.driver" -> "org.postgresql.Driver",
     "db.default.url" -> "jdbc:postgresql://localhost:5432/calendar_test",
     "db.default.user" -> "testapp",
     "db.default.password" -> "temppass"  
  ))) extends WithApplication {
  
  override def around[T: AsResult](t: => T): Result = super.around {
    setupData()
    t
  }
  
  def setupData() = {

  }
}