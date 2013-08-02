package yourcast

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class BasicYourcastSimulation extends Simulation {
  // your code starts here
  val scn = scenario("My Very First Gatling Scenario")
            .exec(http("Yourcast Client")
              .get("http://erebe-vm1.unice.fr:8080"))
            .pause(5)
              
  
  setUp(scn.users(10))
  // your code ends here
}

