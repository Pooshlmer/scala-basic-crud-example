package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import anorm._
import anorm.SqlParser._
import models._
import services.UserService

import org.joda.time.DateTime
import java.sql.Timestamp

import scala.collection.mutable.HashMap

// Controller class for Users, implements login and create

object Users extends Controller {
  
  val loginform = Form(
    mapping(
      "id" -> ignored(0),
      "email" -> email,
      "username" -> ignored(""),
      "password" -> nonEmptyText,
      "role" -> ignored(""),
      "timezone" -> ignored(0)
    )
    (User.apply)(User.unapply)
  )
  
  val createform = Form(
    mapping(
      "id" -> ignored(0),
      "email" -> email,
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "role" -> ignored(""),
      "timezone" -> number(min = -12, max = 12)
    )
    (User.apply)(User.unapply)  
  )
  
  // Add a user to the database
  def createuserinit = Action { implicit request =>
    Ok(views.html.users.create(createform))
  }
  def createuser = Action { implicit request =>
    createform.bindFromRequest.fold(
      errors => BadRequest(views.html.users.create(errors)),
      user => {
        UserService.insertUser(user)
        Redirect(routes.Events.list).withCookies(Cookie("timezone", user.timezone.toString(), Option(86400), "/", None, false, false))
      }
    )
  }
  
  def logininit (urlreturn: String) = Action { implicit request =>
    Ok(views.html.users.login(loginform, urlreturn))
  }  
  def login (urlreturn: String) = Action { implicit request =>
    
    loginform.bindFromRequest.fold(
      errors => BadRequest(views.html.users.login(errors, urlreturn)),
      user => {
        DB.withConnection { implicit c =>
          val dbUser = UserService.selectUser(user.email)
          dbUser match {
            case Some(actualUser) => {
              if (user.password == actualUser.password) {
                Redirect(urlreturn).withSession( 
                    "email" -> actualUser.email, "name" -> actualUser.username, "role" -> actualUser.role,
                    "timezone" -> actualUser.timezone.toString())
                    .withCookies(Cookie("timezone", actualUser.timezone.toString(), Option(86400), "/", None, false, false))
              } else {
                BadRequest(views.html.users.login(loginform.withError("unknown", "Incorrect email or password"), urlreturn))
              }
                  
            }
            case None => BadRequest(views.html.users.login(loginform.withError("unknown", "Incorrect email or password"), urlreturn))
          }
          
        }
      }
    )
  }
  
  def logout = Action { implicit request =>
    Redirect(routes.Users.logininit("")).withNewSession
  }
}