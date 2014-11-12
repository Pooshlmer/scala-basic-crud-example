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
import models.Event

import org.joda.time.DateTime
import java.sql.Timestamp

import scala.collection.mutable.HashMap

object Events extends Controller {
  
  val form = Form(
    mapping(
      "id" -> ignored(0),
      "title" -> nonEmptyText,
      "startTime" -> jodaDate("yyyy/MM/dd HH:mm"),
      "endTime" -> jodaDate("yyyy/MM/dd HH:mm"),
      "streamLink" -> nonEmptyText
  )(Event.apply)(Event.unapply))
  
  def day(dayToFind: String) = Action { implicit request =>
    
    val timezone = getTimezone()
    val dayArray = dayToFind.split('-')
    val dateTime = new DateTime(dayArray(0).toInt, dayArray(1).toInt, dayArray(2).toInt, 0, 0).plusHours(timezone)
    val dateTime2 = dateTime.plusDays(1)
    val timestamp1 = dateTime.toDate()
    val timestamp2 = dateTime.toDate()
    
    DB.withConnection { implicit c =>
      val events =  
        SQL"""
          SELECT * FROM event WHERE start_time >= $timestamp1 AND start_time <= $timestamp2
        """.as(Event.parser(timezone) *)
      
      Ok(views.html.events.day(events))
    }
  }
  
  def list = Action { implicit request =>
    
    val timezone = getTimezone()
    
    DB.withConnection { implicit c =>
      val events = SQL(
        """
          SELECT * FROM event
        """
      ).as(Event.parser(timezone) *)

      Ok(views.html.events.list(events))
    }
    
  }
  
  def add = Action {
    Ok(views.html.events.add(form, List()))
  }
  def save = Action{ implicit request =>
    val event = form.bindFromRequest.get
    
    val timezone = getTimezone()
    
    val timestampstart = event.startTime.minusHours(timezone).toDate()
    val timestampend = event.endTime.minusHours(timezone).toDate()
    DB.withConnection { implicit c =>
      val result = SQL(
        """
          INSERT INTO event(title, stream_link, start_time, end_time) VALUES 
          ({title}, {streamLink}, {startTime}, {endTime})
        """
      ).on("title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
        "endTime" -> timestampend).executeInsert()
    }
    
    Redirect(routes.Events.list)
  }

  def edit(id: Int) = Action { implicit request =>
    
    val timezone = getTimezone()
    
    DB.withConnection { implicit c =>
      val event = SQL(
        """
          SELECT * FROM event WHERE id = {id}
        """
      ).on("id" -> id).as(Event.parser(timezone).single)

      val bindedForm = form.fill(event)
      
      Ok(views.html.events.edit(bindedForm, List()))
    }    
  }  
  def update(id: Int) = Action { implicit request =>
    val event = form.bindFromRequest.get
    
    Logger.debug(event.toString())
    val timezone = getTimezone()
    val timestampstart = event.startTime.minusHours(timezone).toDate()
    val timestampend = event.endTime.minusHours(timezone).toDate()
    Logger.debug(timestampstart.toString())
    Logger.debug(timestampend.toString())
    DB.withConnection { implicit c =>
      val result = SQL(
        """
          UPDATE event SET (title, stream_link, start_time, end_time) = 
          ({title}, {streamLink}, {startTime}, {endTime}) WHERE id = {id}
        """
      ).on("id" -> id, "title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
        "endTime" -> timestampend).executeInsert()
    }
    Redirect(routes.Events.list)
  }

  def delete(id: Int) = Action { implicit request =>
    DB.withConnection { implicit c =>
      val result = SQL(
        """
          DELETE FROM event WHERE id = {id}
        """
      ).on("id" -> id).execute()
    }
    Redirect(routes.Events.list)
  }  
    
  def getTimezone()(implicit req: RequestHeader) : Int = {
    req.cookies.get("timezone") match {
      case Some(x) => try {
        x.value.toInt
      } catch {
        case e: NumberFormatException => 0
      }
      case None => 0
    }
  }
}