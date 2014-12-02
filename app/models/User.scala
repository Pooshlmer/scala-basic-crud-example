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
  
  def list: List[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM accountuser").as(parser *)
    }
  }
  
  def load(id: Int): Option[User] = {
    DB.withConnection { implicit c =>
      SQL"""SELECT * FROM accountuser WHERE id = $id""".as(parser.singleOpt)
    }
  }
  
  def load(email: String): Option[User] = {
    DB.withConnection { implicit c =>
      SQL"""SELECT * FROM accountuser WHERE email = $email""".as(parser.singleOpt)
    }
  }
  
  def updateTimezone(email: String, timezone: Int) = {
    DB.withConnection { implicit c =>
      SQL"""
        UPDATE accountuser SET timezone = $timezone WHERE email = $email
      """.executeInsert()
    }
  }
}