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
import services.EventService
import util.SecurityAction
import util.SecurityRole

import org.joda.time.DateTime
import java.sql.Timestamp

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
    
    val events = EventService.selectEventsFromPeriod(dateTime, dateTime2, timezone)
    Ok(views.html.events.day(events))    
  }
  
  def list = Action { implicit request =>
    
    val timezone = getTimezone()
    
    val eventList = EventService.selectAllEvents(timezone)
    // This function renders a page, they have to be called <name>.scala.html
    Ok(views.html.events.list(eventList))   
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
        
        EventService.insertEvent(event, timezone, request.session.get("email").getOrElse(""))
        Redirect(routes.Events.list)
      }
    )
  })

  def edit(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.edit(id).toString(), { email => implicit request =>
    
    val timezone = getTimezone()
    val event = EventService.selectEvent(id, timezone)
    
    if (event == None) {
      NotFound(<h1>Event id not found</h1>)
    } else {
      // This fills the form for viewing when editing an event
      if (SecurityRole.checkPermissions(SecurityRole.CAN_EDIT, event.get.owner)) {
        val bindedForm = form.fill(event.get)
        val gamesList = event.get.games.map(x => x.game_id)
        //Logger.debug(bindedForm.toString())
        Ok(views.html.events.edit(bindedForm, id, gamesList, Game.list))
      }
      else {
        NotFound(<h1>You do not have permission to edit this event</h1>)
      }
    } 
  })
  def update(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.edit(id).toString(), { email => implicit request =>
    val timezone = getTimezone()
    val eventFromDB = EventService.selectEvent(id, timezone)
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
            EventService.updateEvent(id, event, timezone)            
            Redirect(routes.Events.list)
          }
        )
      } else {
        NotFound(<h1>You do not have permission to edit this event</h1>)
      }
    }
  })

  def delete(id: Int) = util.SecurityAction.isAuthenticated(routes.Events.delete(id).toString(), { email => implicit request =>
    val eventFromDB = EventService.selectEvent(id, 0)
    if (eventFromDB == None) {
      NotFound(<h1>Event id not found</h1>)
    } else {
      if (SecurityRole.checkPermissions(SecurityRole.CAN_EDIT, eventFromDB.get.owner)) {
    
        EventService.deleteEvent(id)
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