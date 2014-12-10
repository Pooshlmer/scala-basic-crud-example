package models

import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._

// This class defines a User, 1 or more of which are hosted by an Event

case class User(id: Int, email: String, username: String, password: String, role: String, timezone: Int) {
}

object User {
  
  // Generic parser for getting row data from an SQL database
  val parser = {
    get[Int]("id") ~
    get[String]("email") ~
    get[String]("username") ~
    get[String]("password") ~
    get[String]("role") ~
    get[Int]("timezone") map {
      case id ~ email ~ username ~ password ~ role ~ timezone => User(id, email, username, password, role, timezone)
    }
  }  

}