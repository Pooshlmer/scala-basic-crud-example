package controllers

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._

import org.joda.time.DateTime

import models.Event
import models.EventGame
import services._
import util.ExternalDBApp

// Testing controllers.Events
// Mockito is used here to replace the EventService database
// calls with dummy data, since all we care about in this
// test is what happens with the data
@RunWith(classOf[JUnitRunner])
class EventControllerTest extends Specification with Mockito {

  sequential
  
  val egames = List(EventGame(0, 0, 1, 4))
  val eventList = List(Event(0, "title", DateTime.now(), DateTime.now().plusHours(1), "streamLink", egames, "owner"))
  
  val formdata = Map(
    "id" -> "0",
    "title" -> "formtitle",
    "startTime" -> "2014/09/09 00:00",
    "endTime" -> "2014/09/09 00:00",
    "streamLink" -> "formlink",
    "games[]" -> "1")
  "Event Controller" should {
    
    // You can mock a class and replace its methods with stubs
    // Useful for when you don't want to test the database
    // portion
    "day method for empty day" in {
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectEventsFromPeriod(any[DateTime], any[DateTime], any[Int]) returns List()
      val eController = new Events(mockEventService)
      val result = eController.day("2014-09-09")(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("No events found")
    }    
    "day method for full day" in {
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectEventsFromPeriod(any[DateTime], any[DateTime], any[Int]) returns eventList
      val eController = new Events(mockEventService)
      val now = DateTime.now()
      val todayString = now.year().getAsString() + "-" + now.monthOfYear().getAsString() + "-" + now.dayOfMonth().getAsString() 
      val result = eController.day(todayString)(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("title")
      contentAsString(result) must contain ("streamLink")
      contentAsString(result) must contain ("1 4")
    }
    
    "list method for empty list" in {
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectAllEvents(any[Int]) returns List()
      val eController = new Events(mockEventService)
      val result = eController.list()(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("No events found")
    }    
    "list method for full list" in {
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectAllEvents(any[Int]) returns eventList
      val eController = new Events(mockEventService)
      val result = eController.list()(FakeRequest())
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("title")
      contentAsString(result) must contain ("streamLink")
      contentAsString(result) must contain ("1 4")
    }
    
    // Should be the same for other authenticated methods,
    // they are all extending the same class
    "add method when not authenticated" in {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      val result = eController.add()(FakeRequest()).run
      
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Users.logininit(routes.Events.add.toString()).toString())    
    }    
    // You need WithApplication when mocking sessions
    "add method when authenticated" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      val result = eController.add()(FakeRequest().withSession(("email", "asdf"))).run
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("Add a new Event")
    }    
    // Here was a trap I fell into:
    // You cannot call the controller action directly if
    // it has data in the body, such as a form. It is the
    // route function which takes that data and feeds it
    // into the request. You can emulate it by using
    // Helpers.call
    "save method" in new ExternalDBApp {
      
      val mockEventService = mock[EventServiceTrait]
      mockEventService.insertEvent(any[Event], any[Int], any[String]) returns 0
      val eController = new Events(mockEventService)
      val req = FakeRequest(POST, "/events")
        .withSession(("email", "asdf"))
        .withFormUrlEncodedBody(
        formdata.toSeq:_*
        )
      val result = call(eController.save, req)
      
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Events.list.toString())      
    }
    "save method with invalid form" in new ExternalDBApp {
      
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      val req = FakeRequest(POST, "/events")
        .withSession(("email", "asdf"))
        .withFormUrlEncodedBody(
        (formdata - "title").toSeq:_*
        )
      val result = call(eController.save, req)
      
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain ("Add a new Event")
    }
    
    "edit method without proper permissions" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val result = eController.edit(0)(FakeRequest().withSession(("email", "asdf"))).run
      
      status(result) must equalTo(NOT_FOUND)
    }    
    "edit method" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val result = eController.edit(0)(FakeRequest().withSession(("email", "owner"), ("role", "basic"))).run
      
      status(result) must equalTo(OK)
      contentAsString(result) must contain ("Edit Event")
    }
    "update method" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val eController = new Events(mockEventService)
      val req = FakeRequest(POST, "/events/0")
        .withSession(("email", "owner"), ("role", "basic"))
        .withFormUrlEncodedBody(
        formdata.toSeq:_*
        )
      val result = call(eController.update(0), req)
      
      //Logger.debug(contentAsString(result))
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Events.list.toString())      
    }
    "update method without proper permissions" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val result = eController.update(0)(FakeRequest().withSession(("email", "asdf"))).run
      
      status(result) must equalTo(NOT_FOUND)
    }
    "update method with invalid form" in new ExternalDBApp {
      
      val mockEventService = mock[EventServiceTrait]
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val eController = new Events(mockEventService)
      val req = FakeRequest(POST, "/events/0")
        .withSession(("email", "owner"), ("role", "basic"))
        .withFormUrlEncodedBody(
        (formdata - "title").toSeq:_*
        )
      val result = call(eController.update(0), req)
      
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain ("Edit Event")
    }
    
    "delete method without proper permissions" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val result = eController.delete(0)(FakeRequest().withSession(("email", "asdf"))).run
      
      status(result) must equalTo(NOT_FOUND)
    }    
    "delete method" in new ExternalDBApp {
      val mockEventService = mock[EventServiceTrait]
      val eController = new Events(mockEventService)
      mockEventService.selectEvent(any[Int], any[Int]) returns Some(eventList.head)
      val result = eController.delete(0)(FakeRequest().withSession(("email", "owner"), ("role", "basic"))).run
      
      val nextUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(routes.Events.list.toString()) 
    }        
  }
}
