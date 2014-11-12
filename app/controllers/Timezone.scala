package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.Play.current
import anorm._

import scala.collection.mutable.HashMap

object Timezone extends Controller {
  
  val timezoneform = Form(
    single(
      "timezone" -> number
    )
  )
  
  def updateTimezone = Action { implicit request =>
    val timezone = timezoneform.bindFromRequest.get
    Redirect(routes.Events.list).withCookies(Cookie("timezone", timezone.toString(), Option(86400), "/", None, false, false))
  }
}