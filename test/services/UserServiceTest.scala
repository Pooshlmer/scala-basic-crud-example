package services

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import org.joda.time.DateTime

import models.User
import util.ExternalDBApp

@RunWith(classOf[JUnitRunner])
class UserServiceTest extends Specification {

  sequential
  
  var insertId = 0
  
  "UserService" should {
    
    "insert into database" in new ExternalDBApp{
      val testuser = User(0, "testemail@test.com", "testuser", "testpass", "user", 0)
      val insertLong = UserService.insertUser(testuser).getOrElse(-1L)
      insertId = insertLong.toInt
      insertId must be_>=(0)
      val userList = UserService.selectAllUsers()
      userList.size must equalTo(2)
    }
    
    "select specific record" in new ExternalDBApp {
      UserService.selectUser("testemail@test.com") match {
        case None => failure
        case Some(x) => {
          x.email must equalTo("testemail@test.com")
          x.username must equalTo("testuser")
        }
      }
    }
    
    "update database" in new ExternalDBApp {
      val testUser = User(0, "newemail@test.com", "newuser", "newpass", "user", 0)
      UserService.updateUser(insertId, testUser)
      UserService.selectUser("newemail@test.com") match {
        case None => failure
        case Some(x) => {
          x.email must equalTo("newemail@test.com")
          x.username must equalTo("newuser")
        }
      }
    }
    
    "delete record" in new ExternalDBApp {
      UserService.deleteUser(insertId)
      UserService.selectAllUsers().size must equalTo(1)
    }
  }
}
