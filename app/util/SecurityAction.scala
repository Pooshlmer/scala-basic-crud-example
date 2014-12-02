package util

import play.api.mvc._
import play.api._ 
import play.api.mvc.Results._
import play.api.libs.iteratee._
import scala.concurrent.Future

import models.User

// This action implements the builtin user authentication from play.api.mvc.Security
// Copied it straight from the Security comments

object SecurityAction {
  def userinfo(request: RequestHeader) = request.session.get("email")
  // To import a route outside a Controller, use controllers.routes.X
  def onUnauthorized(request: RequestHeader, urlreturn: String) = {
    Results.Redirect(controllers.routes.Users.logininit(urlreturn))
  }
  def isAuthenticated(urlreturn: String, f: => String => Request[AnyContent] => Result) = {
    myAuthenticated(urlreturn, userinfo, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }  
  
  // Took the function from Security and added a urlreturn parameter to go to after login
  def myAuthenticated[A](
    urlreturn: String,
    userinfo: RequestHeader => Option[A],
    onUnauthorized: (RequestHeader, String) => Result)(action: A => EssentialAction): EssentialAction = {
    EssentialAction { request =>
      userinfo(request).map { user =>
        action(user)(request)
      }.getOrElse {
        Done(onUnauthorized(request, urlreturn), Input.Empty)
      }
    }
  }
}