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

import org.joda.time.DateTime
import java.sql.Timestamp

import scala.collection.mutable.HashMap

// Controller class for Events, supports the basic CRUD operations

object Events extends Controller {
  
  val gamesConstraint = Constraint[Seq[Int]](Some("games.constraint"), Nil)(games =>
    if (games.isEmpty) {
      Invalid(ValidationError("Each event must have at least one game"))
    } else {
      Valid
    }
  )
  
  // You can use ignored for things like id where you don't want user interaction
  // The jodaDate can take a pattern parameter to control how it is displayed/input
  val form = Form(
    mapping(
      "id" -> number,
      "title" -> nonEmptyText,
      "startTime" -> jodaDate("yyyy/MM/dd HH:mm"),
      "endTime" -> jodaDate("yyyy/MM/dd HH:mm"),
      "streamLink" -> nonEmptyText,
      // This is a sequence of numbers, passed in as games[]
      // This comment is mainly here to complain that list didn't work
      "games" -> seq(number).verifying(gamesConstraint)
    )
    // If you have a straightforward mapping, you can put (<class>.apply)(<class>.unapply) here
    // when using case classes. Otherwise implement your own methods
    {(id, title, startTime, endTime, streamLink, games) =>
      {
        var eventgames = List[EventGame]()
        for (game <- games) {
          eventgames = EventGame(0, id, game, EventGame.BOTTOMTIER) :: eventgames
        }
        Event(id, title, startTime, endTime, streamLink, eventgames)
      }
    }
    (event => Some(event.id, event.title, event.startTime, event.endTime, event.streamLink, event.games.map(_.game_id)))
  )
  
  // Prints out the events running on a particular day
  // implicit is kind of like defining a global, if a method
  // needs a request object that is not explicitly passed, it
  // can use a implicit variable (this is still in the function signature)
  def day(dayToFind: String) = Action { implicit request =>
    
    val timezone = getTimezone()
    val dayArray = dayToFind.split('-')
    val dateTime = new DateTime(dayArray(0).toInt, dayArray(1).toInt, dayArray(2).toInt, 0, 0).plusHours(timezone)
    val dateTime2 = dateTime.plusDays(1)
    val timestamp1 = dateTime.toDate()
    val timestamp2 = dateTime.toDate()
    
    // You can use $<variable> with the SQL""" syntax to interpolate in the string itself
    // Interpolate expressions with ${expr}
    // The parser uses * to get all rows, it then puts them into a list
    // The as function executes the select with the given parser
    // Here c is used in the SQL call as the connection
    DB.withConnection { implicit c =>
      val events =  
        SQL"""
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id)
            WHERE (start_time >= $timestamp1 AND start_time <= $timestamp2) OR
            (end_time >= $timestamp1 AND end_time <= $timestamp2)
        """.as(Event.fullparser(timezone) *)
      
      val eventList = Event.convertFullParser(events)
      Ok(views.html.events.day(eventList))
    }
  }
  
  def list = Action { implicit request =>
    
    val timezone = getTimezone()
    
    DB.withConnection { implicit c =>
      val events = SQL(
        """
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id)
        """
      ).as(Event.fullparser(timezone) *)

      val eventList = Event.convertFullParser(events)
      // This function renders a page, they have to be called <name>.scala.html
      Ok(views.html.events.list(eventList))
    }
    
  }
  
  def add = Action {
    Ok(views.html.events.add(form, List(), Game.list))
  }
  def save = Action{ implicit request =>
    form.bindFromRequest.fold(
      errors => BadRequest(views.html.events.add(errors, List(), Game.list)),
      event => {
        //Logger.debug(event.toString())
        
        val timezone = getTimezone()
        
        val timestampstart = event.startTime.minusHours(timezone).toDate()
        val timestampend = event.endTime.minusHours(timezone).toDate()
        
        // You can also use on() if you want to define strings yourself
        DB.withConnection { implicit c =>
          val result = SQL(
            """
              INSERT INTO event(title, stream_link, start_time, end_time) VALUES 
              ({title}, {streamLink}, {startTime}, {endTime})
            """
          ).on("title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
            "endTime" -> timestampend).executeInsert()
          val insertId = result.get.toInt
          addGames(insertId, event.games)
        }
        
        Redirect(routes.Events.list)
      }
    )
  }
  
  // Add or delete the EventGame references when you add or delete an Event
  def addGames(eventId: Int, eventGames: List[EventGame])(implicit c: java.sql.Connection) {
    for (eventGame <- eventGames) {
      SQL"""INSERT INTO event_game_xref(event_id, game_id, tier)
        VALUES($eventId, ${eventGame.game_id}, ${eventGame.tier})""".executeInsert()
    }
  }
  def deleteGames(eventId: Int)(implicit c: java.sql.Connection) {
    SQL"""DELETE FROM event_game_xref WHERE event_id = $eventId""".execute
  }

  def edit(id: Int) = Action { implicit request =>
    
    val timezone = getTimezone()
     
    DB.withConnection { implicit c =>
      val event = SQL(
        """
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id) WHERE id = {id}
        """
      ).on("id" -> id).as(Event.fullparser(timezone) *)

      val eventList = Event.convertFullParser(event)
      
      if (eventList.isEmpty) {
        NotFound(<h1>Event id not found</h1>)
      } else {
        // This fills the form for viewing when editing an event
        val bindedForm = form.fill(eventList.head)
        Logger.debug(bindedForm.toString())
        Ok(views.html.events.edit(bindedForm, id, eventList.head.games, Game.list))
      }
    }    
  }  
  def update(id: Int) = Action { implicit request =>
    val timezone = getTimezone()
    val eventFromDB = Event.load(id, timezone)
    if (eventFromDB == None) {
      NotFound(<h1>Event id not found</h1>)
    } else {
      // This is how to autofill the form defined above
      form.bindFromRequest.fold(
        errors => {
          Logger.debug(errors.toString())
          BadRequest(views.html.events.edit(errors, id, eventFromDB.get.games, Game.list))
        },
        event => {
          Logger.debug(event.toString())
          val timestampstart = event.startTime.minusHours(timezone).toDate()
          val timestampend = event.endTime.minusHours(timezone).toDate()
          
          DB.withConnection { implicit c =>
            val result = SQL(
              """
                UPDATE event SET (title, stream_link, start_time, end_time) = 
                ({title}, {streamLink}, {startTime}, {endTime}) WHERE id = {id}
              """
            ).on("id" -> id, "title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
              "endTime" -> timestampend).executeUpdate()
            deleteGames(id)
            addGames(id, event.games)
          }
          Redirect(routes.Events.list)
        }
      )
    }
  }

  def delete(id: Int) = Action { implicit request =>
    DB.withConnection { implicit c =>
      val result = SQL(
        """
          DELETE FROM event WHERE id = {id}
        """
      ).on("id" -> id).execute()
      deleteGames(id)
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