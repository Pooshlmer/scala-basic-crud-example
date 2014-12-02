package util

import play.api.mvc.RequestHeader
import play.api.Logger

// This class implements roles, which control access to actions for users
object SecurityRole {
  
  val CAN_EDIT = List("admin")
  
  def checkPermissions(validRoles: List[String])(implicit req: RequestHeader) : Boolean = {
    val role = req.session.get("role")
    role match {
      case None => false
      case Some(x) => validRoles.contains(x)
    }
  }
  
  // This allows the owner of an object to change it
  def checkPermissions(validRoles: List[String], owner: String)(implicit req: RequestHeader) : Boolean = {
    val role = req.session.get("role")
    val sessionUser = req.session.get("email").getOrElse("")
    role match {
      case None => false
      case Some(x) => validRoles.contains(x) && sessionUser == owner 
    }
  }
}