package views

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Session
import org.joda.time.DateTime

import models.Event
import models.Game
import controllers.Events
import models.EventGame
import util.ExternalDBApp

@RunWith(classOf[JUnitRunner])
class EventViewTest extends Specification {
  
  var insertId = 0
  val gameList = List(Game(1, "League of Legends"), Game(2, "Dota 2"))
  val egames = List(EventGame(0, 0, 1, 4))
  val testEvent = Event(0, "title", DateTime.now(), DateTime.now().plusHours(1), "streamLink", egames, "owner")
  
  "Event Views" should {
    
    "render list template" in new WithApplication { 

      val session = new Session(Map())
      val html = views.html.events.list(List(testEvent))(session)
      
      contentAsString(html) must contain ("title")
      contentAsString(html) must contain ("streamLink")
      contentAsString(html) must contain ("1 4")
    }
    
    "render edit template" in new WithApplication {
      
      val html = views.html.events.edit(Events.form(FakeRequest()).fill(testEvent), 0, List(1), gameList)
      
      contentAsString(html) must contain ("title")
      contentAsString(html) must contain ("streamLink")
    }
    
    "render daily template" in new WithApplication {
      
      val session = new Session(Map())
      val html = views.html.events.list(List(testEvent))(session)
      
      contentAsString(html) must contain ("title")
      contentAsString(html) must contain ("streamLink")
      contentAsString(html) must contain ("1 4")
    }
    

  }
}
