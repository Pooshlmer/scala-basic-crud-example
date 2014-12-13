import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * This is not working at the moment, something to do with
 * drivers. When I try HTMLUNIT it gives me Javascript errors
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {
  
  "Application" should {
/*
    "work from within a browser" in new WithBrowser(webDriver = WebDriverFactory(FIREFOX), FakeApplication(), 9000) {

      browser.goTo("http://localhost:9000")

      browser.pageSource must contain("All events")
    }
    * 
    */
  }
}
