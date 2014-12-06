package services

import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import models.User

object UserService {
  def insertUser(user: User) = {
    DB.withConnection { implicit c =>
      val result =
        SQL"""
        INSERT INTO accountuser(email, username, password, role, timezone) VALUES
        (${user.email}, ${user.username}, ${user.password}, 'basic', ${user.timezone})
        """.executeInsert()
    }
  }
  
  def selectUser(email: String) = {
    DB.withConnection { implicit c => 
      val result = SQL"""
      SELECT * FROM accountuser WHERE email = $email
      """.as(User.parser singleOpt)
      result
    }
  }
}