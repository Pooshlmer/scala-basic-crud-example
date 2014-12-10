package services

import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import anorm._
import anorm.SqlParser._
import models.Event
import models.EventGame

import org.joda.time.DateTime
import java.sql.Timestamp

// Database functions for Events, mostly used so we can mock these functions in tests
// All events are modifed by timezone to return them in the user's local time, they are
// stored GMT(+0:00)
object EventService {
  
  // Select all events in the database
  // You can use $<variable> with the SQL""" syntax to interpolate in the string itself
  // Interpolate expressions with ${expr}
  // The parser uses * to get all rows, it then puts them into a list
  // The as function executes the select with the given parser
  // Here c is used in the SQL call as the connection
  def selectAllEvents(timezone: Int) = {
    DB.withConnection { implicit c =>
      val events = SQL(
        """
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id) WHERE deleted = false 
        """
      ).as(Event.fullparser(timezone) *)
      Event.convertFullParser(events)
    }
  }
  
  // Select an event based on id
  def selectEvent(id: Int, timezone: Int) = {
    DB.withConnection { implicit c =>
      val event = SQL(
        """
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id) WHERE deleted = false AND id = {id}
        """
      ).on("id" -> id).as(Event.fullparser(timezone) *)
  
      val eventList = Event.convertFullParser(event)
      if (eventList.size > 1) {
        Logger.error("something is very wrong")
        None
      } else if (eventList.size == 1) {
        Some(eventList.head)
      } else {
        None
      }
    }
  }
  
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

  // Get events running within a certain time period
  def selectEventsFromPeriod(startTime: DateTime, endTime: DateTime, timezone: Int) = {
    if (endTime.isBefore(startTime)) List()
    else {
      DB.withConnection { implicit c =>
        val events =  
          SQL"""
          SELECT * FROM event e JOIN event_game_xref eg ON (e.id = eg.event_id)
          WHERE deleted = false AND (start_time >= ${startTime.toDate()} AND start_time <= ${endTime.toDate()}) OR
          (end_time >= ${startTime.toDate()} AND end_time <= ${endTime.toDate()})
          """.as(Event.fullparser(timezone) *)
          
          Event.convertFullParser(events)
      }
    }
  }
  
  // Inserts an event into the database
  def insertEvent(event: Event, timezone: Int, owner: String) = {

    val timestampstart = event.startTime.minusHours(timezone).toDate()
    val timestampend = event.endTime.minusHours(timezone).toDate()
    // You can also use on() if you want to define strings yourself
    //Logger.debug("inserting event: " + event.toString())
    //Logger.debug("with owner: " + owner)
    DB.withConnection { implicit c =>
      val result = SQL(
        """
        INSERT INTO event(title, stream_link, start_time, end_time, owner, deleted) VALUES 
        ({title}, {streamLink}, {startTime}, {endTime}, {owner}, false)
        """
        ).on("title" -> event.title, "streamLink" -> event.streamLink, "startTime" -> timestampstart,
            "endTime" -> timestampend, "owner" -> owner).executeInsert()
      val insertId = result.get.toInt
      addGames(insertId, event.games)
      result
    }
  }
  
  def updateEvent(id: Int, event: Event, timezone: Int) = {
    
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
  }
  
  def deleteEvent(id: Int) = {
    DB.withConnection { implicit c =>
      val result = SQL(
        """
          UPDATE event SET deleted = true WHERE id = {id}
        """
      ).on("id" -> id).execute()
      deleteGames(id)
    }
  }
}