package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.Play.current
import anorm._

import models.User

import scala.collection.mutable.HashMap

// This class is used to set the timezone
// Currently implemented using a cookie
object Timezone extends Controller {
  
  val timezoneform = Form(
    single(
      "timezone" -> number(min = -12, max = 12)
    )
  )
  
  def updateTimezone = Action { implicit request =>
    val timezone = timezoneform.bindFromRequest.get
    if (!request.session.get("email").isEmpty) {
      User.updateTimezone(request.session.get("email").get, timezone)
    }
    Redirect(routes.Events.list).withCookies(Cookie("timezone", timezone.toString(), Option(86400), "/", None, false, false))
  }
}