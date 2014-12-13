package services

import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import anorm._
import anorm.SqlParser._
import models.User

object UserService {
  
  def selectAllUsers() = {
    DB.withConnection { implicit c =>
      val users = SQL(
        """
          SELECT * FROM accountuser
        """
      ).as(User.parser *)
      users
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
  
  def insertUser(user: User) = {
    DB.withConnection { implicit c =>
      val result =
        SQL"""
        INSERT INTO accountuser(email, username, password, role, timezone) VALUES
        (${user.email}, ${user.username}, ${user.password}, ${User.ROLE_USER}, ${user.timezone})
        """.executeInsert()
      result
    }
  }
  
  def updateUser(id: Int, user: User) = {
    
    DB.withConnection { implicit c =>
      val result = SQL"""
          UPDATE accountuser SET (email, username, password, role, timezone) = 
          (${user.email}, ${user.username}, ${user.password}, ${user.role}, ${user.timezone}) WHERE id = $id
        """.executeUpdate()
    }
  }
  
  def deleteUser(id: Int) = {
    DB.withConnection { implicit c =>
      val result = SQL"""
        DELETE FROM accountuser WHERE id = $id 
      """.executeUpdate()
    }
  }
  
  def deleteUser(email: String) = {
    DB.withConnection { implicit c =>
      val result = SQL"""
        DELETE FROM accountuser WHERE email = $email 
      """.executeUpdate()
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