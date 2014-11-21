package models

import org.joda.time.DateTime
import anorm._
import anorm.SqlParser._
import util.AnormExtension._

// This class defines an Event, which is held on a date and has at least one Game

// Case classes are a way to avoid some boilerplate and allow pattern matching on the members
case class Event(id: Int, title: String, startTime: DateTime, endTime: DateTime, streamLink: String, games: List[EventGame]) {
}

object Event {
  // A slightly more complex parser with a parameter. The DateTime field is done using a custom class
  // AnormExtension, although in Play 2.4 Jodatime is a native parser class
  def parser(timezone: Int) = {
    get[Int]("id") ~
    get[String]("title") ~
    get[DateTime]("start_time") ~
    get[DateTime]("end_time") ~
    get[String]("stream_link") map {
      case id~title~startTime~endTime~streamLink => 
        Event(id, title, startTime.plusHours(timezone), endTime.plusHours(timezone), streamLink, List())
    }
  }
  
  // A parser for a query joining Events with EventGames
  // Ends up with a bunch of repeated events in tuple form with differing game_ids
  def fullparser(timezone: Int) = {
    get[Int]("id") ~
    get[String]("title") ~
    get[DateTime]("start_time") ~
    get[DateTime]("end_time") ~
    get[String]("stream_link") ~
    get[Int]("game_id") ~
    get[Int]("tier") map {
      case id~title~startTime~endTime~streamLink~game_id~tier => 
        (id, title, startTime.plusHours(timezone), endTime.plusHours(timezone), streamLink, game_id, tier)        
    }
  }
  
  // Converts the results from the fullparser
  def convertFullParser(groupedEvents: List[(Int, String, DateTime, DateTime, String, Int, Int)]) : List[Event] = {
    var eventList = List[Event]()
    for (groupedEvent <- groupedEvents.groupBy(_._1).values) {
      var gameList = List[EventGame]()
      for (singleEvent <- groupedEvent) {
        gameList ::= EventGame(0, singleEvent._1, singleEvent._6, singleEvent._7)
      }
      eventList ::= Event(groupedEvent.head._1, groupedEvent.head._2, groupedEvent.head._3, 
          groupedEvent.head._4, groupedEvent.head._5, gameList)
    }
    return eventList
  }
}