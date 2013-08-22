
import java.util.Calendar
import java.io.PrintWriter
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class BasicYourcastSimulation extends Simulation {
  val start = Calendar.getInstance().getTime().getTime()
  val start_writer = new PrintWriter("/tmp/start_experiment","UTF-8")
  start_writer.println(start)
  start_writer.close()
  val scn = scenario("My Very First Gatling Scenario").exec(session => {
				  val start = Calendar.getInstance().getTime().getTime()
				  val start_writer = new PrintWriter("/tmp/start_experiment","UTF-8")
				  start_writer.println(start)
				  start_writer.close()
				  session
  			})
            .repeat(50) {
                exec(
                http("Yourcast Client")
              .get("http://erebe-vm1.unice.fr:8080"))
            .pause(1)
            }.exec(session => {
				  val end = Calendar.getInstance().getTime().getTime()
				  val end_writer = new PrintWriter("/tmp/end_experiment","UTF-8")
				  end_writer.println(end)
				  end_writer.close()
				  session
            })
  
  setUp(scn.users(10))
  val end = Calendar.getInstance().getTime().getTime()
  val end_writer = new PrintWriter("/tmp/end_experiment","UTF-8")
  end_writer.println(end)
  end_writer.close()
  // your code ends here
}

