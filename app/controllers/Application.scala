package controllers

import play.api._
import play.api.mvc._

// This is the base class called when you go to the base page
object Application extends Controller {

  def index = Action {
    Redirect(routes.Events.list)
  }

}