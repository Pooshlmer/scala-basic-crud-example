package services

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import org.joda.time.DateTime

import models.Event
import models.EventGame
import util.ExternalDBApp

@RunWith(classOf[JUnitRunner])
class EventServiceTest extends Specification {

  sequential
  
  var insertId = 0
  
  "EventService" should {
    
    "select from database" in new ExternalDBApp {
      EventService.selectAllEvents(0).size must equalTo(0)
    }
    
    "insert into database" in new ExternalDBApp{
      val egames = List(EventGame(0, 0, 1, 4))
      val testEvent = Event(0, "title", DateTime.now(), DateTime.now().plusHours(1), "streamLink", egames, "owner")
      val insertLong = EventService.insertEvent(testEvent, 0, "owner").getOrElse(-1L)
      insertId = insertLong.toInt
      insertId must be_>=(0)
      val eventList = EventService.selectAllEvents(0)
      eventList.size must equalTo(1)
    }
    
    "select specific record" in new ExternalDBApp {
      EventService.selectEvent(insertId, 0) match {
        case None => failure
        case Some(x) => {
          x.title must equalTo("title")
          x.streamLink must equalTo("streamLink")
          x.games.length must equalTo(1)
        }
      }
    }
    
    "select time period" in new ExternalDBApp {
      EventService.selectEventsFromPeriod(DateTime.now().plusHours(3), DateTime.now().plusHours(5), 0).length must equalTo(0)
      EventService.selectEventsFromPeriod(DateTime.now().minusHours(3), DateTime.now().plusHours(5), 0).length must equalTo(1)
    }
    
    //TODO Add timezone testing
    
    "update database" in new ExternalDBApp {
      val egames = List(EventGame(0, 0, 1, 4))
      val testEvent = Event(0, "new title", DateTime.now(), DateTime.now().plusHours(1), "streamLink", egames, "owner")
      EventService.updateEvent(insertId, testEvent, 0)
      EventService.selectEvent(insertId, 0) match {
        case None => failure
        case Some(x) => {
          x.title must equalTo("new title")
          x.streamLink must equalTo("streamLink")
          x.games.length must equalTo(1)
        }
      }
    }
    
    "delete record" in new ExternalDBApp {
      EventService.deleteEvent(insertId)
      EventService.selectAllEvents(0).size must equalTo(0)
    }
  }
}
