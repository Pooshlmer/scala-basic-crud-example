package controllers

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._

import org.joda.time.DateTime

import models.User
import services.{UserService, UserServiceTrait}
import util.ExternalDBApp

// Testing controllers.Users
@RunWith(classOf[JUnitRunner])
class UserControllerTest extends Specification with Mockito {

  sequential
  
  val formdata = Map(
    "id" -> "0",
    "email" -> "testemail@test.com",
    "username" -> "testuser",
    "password" -> "testpass",
    "role" -> User.ROLE_USER,
    "timezone" -> "0")
    
  val sampleuser = User(0, "testemail@test.com", "testuser", "testpass", User.ROLE_USER, 0)
  
  "User Controller" should {
    
    // You can mock a class and replace its methods with stubs
    // Useful for when you don't want to test the database
    // portion
    "create init" in {
      val mockUserService = mock[UserServiceTrait]
      val uController = new Users(mockUserService)
      val result = uController.createuserinit()(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("Create a new account")
    }
    "create" in new ExternalDBApp {
      val mockUserService = mock[UserServiceTrait]
      mockUserService.insertUser(any[User]) returns 0
      val uController = new Users(mockUserService)
      val req = FakeRequest(POST, "/create")
        .withFormUrlEncodedBody(
        formdata.toSeq:_*
        )
      val result = call(uController.createuser, req)
      
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Events.list.toString())      
    }
    "create with bad form" in new ExternalDBApp {
      val mockUserService = mock[UserServiceTrait]
      mockUserService.insertUser(any[User]) returns 0
      val uController = new Users(mockUserService)
      val req = FakeRequest(POST, "/create")
        .withFormUrlEncodedBody(
        (formdata - "email").toSeq:_*
        )
      val result = call(uController.createuser, req)
      
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain ("Create a new account")
    }
    
    "login init" in {
      val mockUserService = mock[UserServiceTrait]
      val uController = new Users(mockUserService)
      val result = uController.logininit("asdf")(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("Login")
    }
    "login" in new ExternalDBApp {
      val mockUserService = mock[UserServiceTrait]
      mockUserService.selectUser(any[String]) returns Some(sampleuser)
      val uController = new Users(mockUserService)
      val req = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
        (formdata).toSeq:_*
        )
      val result = call(uController.login("/events"), req)
      
      cookies(result).get("timezone") must not equalTo(None)
      
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Events.list.toString())    
    }
    "login with bad form" in new ExternalDBApp {
      val mockUserService = mock[UserServiceTrait]
      mockUserService.selectUser(any[String]) returns Some(sampleuser)
      val uController = new Users(mockUserService)
      val req = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
        (formdata - "email").toSeq:_*
        )
      val result = call(uController.login("/events"), req)
      
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain ("Login")
    }
    "login with bad password" in new ExternalDBApp {
      val mockUserService = mock[UserServiceTrait]
      mockUserService.selectUser(any[String]) returns Some(sampleuser)
      val uController = new Users(mockUserService)
      val req = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
        (formdata + ("password" -> "alksdj")).toSeq:_*
        )
      val result = call(uController.login("/events"), req)
      
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain ("Login")
    }
  }
}
