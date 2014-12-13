import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import util.ExternalDBApp

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new ExternalDBApp{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new ExternalDBApp{
      val home = route(FakeRequest(GET, "/")).get

      val nextUrl = redirectLocation(home) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must equalTo(controllers.routes.Events.list.toString())
      
      val newResult = route(FakeRequest(GET, nextUrl)).get
      status(newResult) must equalTo(OK)
      contentType(newResult) must beSome.which(_ == "text/html")
      contentAsString(newResult) must contain ("All events")
    }
  }
}
