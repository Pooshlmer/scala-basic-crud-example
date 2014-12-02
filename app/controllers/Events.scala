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
import util.SecurityAction
import util.SecurityRole

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
  // The parameter is useful for passing requests
  // implicit is kind of like defining a global, if a method
  // needs a request object that is not explicitly passed, it
  // can use a implicit variable (this is still in the function signature)
  def form(implicit request:Request[_]) = Form(
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
        Event(id, title, startTime, endTime, streamLink, eventgames, request.session.get("email").getOrElse(""))
      }
    }
    (event => Some(event.id, event.title, event.startTime, event.endTime, event.streamLink, event.games.map(_.game_id)))
  )
  
  // Prints out the events running on a particular day
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
            WHERE deleted = false AND (start_time >= $timestamp1 AND start_time <= $timestamp2) OR
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
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id) WHERE deleted = false 
        """
      ).as(Event.fullparser(timezone) *)

      val eventList = Event.convertFullParser(events)
      // This function renders a page, they have to be called <name>.scala.html
      Ok(views.html.events.list(eventList))
    }
    
  }
  
  // This is a custom action for user authentication, implemented in util.SecurityAction
  def add = util.SecurityAction.isAuthenticated(routes.Events.add.toString(), { email => implicit request =>
    Ok(views.html.events.add(form, Game.list))
  })
  def save = util.SecurityAction.isAuthenticated(routes.Events.add.toString(), { email => implicit request =>
    form.bindFromRequest.fold(
      errors => BadRequest(views.html.events.add(errors, Game.list)),
      event => {
        //Logger.debug(event.toString())
        
        val timezone = getTimezone()
        
        val timestampstart = event.startTime.minusHours(timezone).toDate()
        val timestampend = event.endTime.minusHours(timezone).toDate()
        
        // You can also use on() if you want to define strings yourself
        DB.withConnection { implicit c =>
          val result = SQL(
            """
              INSERT INTO event(title, stream_link, start_time, end_time, owner) VALUES 
              ({title}, {streamLink}, {startTime}, {endTime}, {owner})
            """
          ).on("title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
            "endTime" -> timestampend, "owner" -> request.session.get("owner").getOrElse("")).executeInsert()
          val insertId = result.get.toInt
          addGames(insertId, event.games)
        }
        
        Redirect(routes.Events.list)
      }
    )
  })
  
  // Add or delete the EventGame references when you add or delete an Event
  private def addGames(eventId: Int, eventGames: List[EventGame])(implicit c: java.sql.Connection) {
    for (eventGame <- eventGames) {
      SQL"""INSERT INTO event_game_xref(event_id, game_id, tier)
        VALUES($eventId, ${eventGame.game_id}, ${eventGame.tier})""".executeInsert()
    }
  }
  private def deleteGames(eventId: Int)(implicit c: java.sql.Connection) {
    SQL"""DELETE FROM event_game_xref WHERE event_id = $eventId""".execute
  }

  def edit(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.edit(id).toString(), { email => implicit request =>
    
    val timezone = getTimezone()
     
    DB.withConnection { implicit c =>
      val event = SQL(
        """
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id) WHERE deleted = false AND id = {id}
        """
      ).on("id" -> id).as(Event.fullparser(timezone) *)

      val eventList = Event.convertFullParser(event)
      
      if (eventList.isEmpty) {
        NotFound(<h1>Event id not found</h1>)
      } else {
        // This fills the form for viewing when editing an event
        if (SecurityRole.checkPermissions(SecurityRole.CAN_EDIT, eventList.head.owner)) {
          val bindedForm = form.fill(eventList.head)
          val gamesList = eventList.head.games.map(x => x.game_id)
          //Logger.debug(bindedForm.toString())
          Ok(views.html.events.edit(bindedForm, id, gamesList, Game.list))
        }
        else {
          NotFound(<h1>You do not have permission to edit this event</h1>)
        }
      }
    }    
  })
  def update(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.edit(id).toString(), { email => implicit request =>
    val timezone = getTimezone()
    val eventFromDB = Event.load(id, timezone)
    if (eventFromDB == None) {
      NotFound(<h1>Event id not found</h1>)
    } else {
      if (SecurityRole.checkPermissions(SecurityRole.CAN_EDIT, eventFromDB.get.owner)) {
        // This is how to autofill the form defined above
        form.bindFromRequest.fold(
          errors => {
            //Logger.debug(errors.toString())
            // This construct gets all the values passed into the games[] parameter and
            // converts them to a List[Int] to fill the checkboxes when returning with an error
            val gamesList = errors.apply("games").indexes.map(i => errors.apply("games[" + i + "]").value.get.toInt).toList
            BadRequest(views.html.events.edit(errors, id, gamesList, Game.list))
          },
          event => {
            //Logger.debug(event.toString())
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
      } else {
        NotFound(<h1>You do not have permission to edit this event</h1>)
      }
    }
  })

  def delete(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.delete(id).toString(), { email => implicit request =>
    val eventFromDB = Event.load(id, 0)
    if (eventFromDB == None) {
      NotFound(<h1>Event id not found</h1>)
    } else {
      if (SecurityRole.checkPermissions(SecurityRole.CAN_EDIT, eventFromDB.get.owner)) {
    
        DB.withConnection { implicit c =>
          val result = SQL(
            """
              UPDATE event SET deleted = true WHERE id = {id}
            """
          ).on("id" -> id).execute()
          deleteGames(id)
        }
        Redirect(routes.Events.list)
      } else {
        NotFound(<h1>You do not have permission to delete this event</h1>)
      }
    }
  }  )
    
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