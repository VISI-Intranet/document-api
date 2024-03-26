package route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import domain._
import repo.{CourseRepository, UserRepository}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CourseRoute(implicit val courseRepo: CourseRepository, val userRepo:UserRepository, val ex:ExecutionContext) extends JsonSupport {
  private val fields: List[String] = List(
    "courseid",
    "name",
    "lastname",
    "userids",
    "enrollmentdate"
  )

  val route: Route = pathPrefix("course") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          validate(fields.contains(field),
            s"Вы ввели неправильное имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}") {
            val convertedParameter = if (parameter.matches("-?\\d+")) parameter.toInt else parameter
            onComplete(courseRepo.customFilter(field, convertedParameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось сделать запрос! ${ex.getMessage}")
            }
          }
        }
      } ~
      get {
        onComplete(courseRepo.getAllCourses()) {
          case Success(result) =>
            val userList = result
            complete(StatusCodes.OK, userList)
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
        post {
          entity(as[Course]) { newCourse => {
            onComplete(courseRepo.addCourse(newCourse)) {
              case Success(newCourseId) =>
                complete(StatusCodes.Created, s"ID нового курса $newCourseId")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось создать курса: ${ex.getMessage}")
            }
          }
          }
        }
    } ~
    path(Segment) { courseId =>
      get {
        onComplete(courseRepo.getCourseById(courseId)) {
          case Success(Some(user)) => complete(StatusCodes.OK, user)
          case Success(None) => complete(StatusCodes.NotFound, s"Курса под ID $courseId не существует!")
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
        put {
          entity(as[CourseUpdate]) { updatedCourse => {
            onComplete(courseRepo.updateCourse(courseId, updatedCourse)) {
              case Success(updatedCourseId) =>
                complete(StatusCodes.OK, updatedCourseId)
              case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
            }
          }
          }
        } ~
        delete {
          onComplete(courseRepo.deleteCourse(courseId)) {
            case Success(deletedCourseId) =>
              complete(StatusCodes.OK, s"ID удаленного Курса: $deletedCourseId")
            case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
          }
        }
    }
  }
}
