import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.ExecutionContextExecutor
import repo._
import route._


object Main extends App {

  // Создание акторной системы
  implicit val system: ActorSystem = ActorSystem("MyAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  private val mongoClient = MongoClient()
  implicit val db: MongoDatabase = mongoClient.getDatabase("univer")

  implicit val courseRepository: CourseRepository = new CourseRepository()
  implicit val userRepository: UserRepository = new UserRepository()
  implicit val documentationRepository: DocumentationRepository = new DocumentationRepository()


  private val courseRoute = new CourseRoute()
  private val userRoute = new UserRoute()
  private val docRoute = new DocRoute()


  // Добавление путей
  private val allRoutes = {
    courseRoute.route~
    userRoute.route~
    docRoute.route
  }

  // Старт сервера
  private val bindingFuture = Http().bindAndHandle(allRoutes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  // Остановка сервера при завершении приложения
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
