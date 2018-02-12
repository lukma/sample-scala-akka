import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import rest.MainRest
import utils._

/**
  * Created by Gayo on 10/27/2016.
  */
object Boot extends App with RouteConcatenation {
  val modules = new ConfigurationModuleImpl with ActorModuleImpl with PersistenceModuleImpl
  implicit val system = modules.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = modules.system.dispatcher

  if (false) {
    modules.generateDDL()
  }

  if (false) {
    modules.generateData()
  }

  val bindingFuture = Http().bindAndHandle(new MainRest(modules).routes, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/")
}