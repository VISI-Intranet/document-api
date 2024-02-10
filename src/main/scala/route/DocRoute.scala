package route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import domain._
import repo.{CourseRepository, DocumentationRepository, UserRepository}
import spray.json.DefaultJsonProtocol.{jsonFormat6, jsonFormat7}
import spray.json.RootJsonFormat
import route.JsonSupport

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class DocRoute(implicit val docuRepo: DocumentationRepository, val userRepo:UserRepository, val ex:ExecutionContext) extends JsonSupport {
  private val fields: List[String] = List(
    "docid",
    "title",
    "userid",
    "courseid",
    "dateuploaded",
    "documenttype",
    "tags",
    "fileURL"
  )
  val route: Route = pathPrefix("documentation") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          validate(fields.contains(field),
            s"Вы ввели неправильное имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}") {
            val convertedParameter = if (parameter.matches("-?\\d+")) parameter.toInt else parameter
            onComplete(docuRepo.customFilter(field, convertedParameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось сделать запрос! ${ex.getMessage}")
            }
          }
        }
      } ~
      get {
        onComplete(docuRepo.getAllDocumentation()) {
          case Success(result) =>
            val userList = result
            complete(StatusCodes.OK, userList)
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
        post {
          entity(as[Documentation]) { newDocumentation => {
            onComplete(docuRepo.addDocumentation(newDocumentation)) {
              case Success(newDocumentationId) =>
                complete(StatusCodes.Created, s"ID нового документа $newDocumentationId")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось создать документа: ${ex.getMessage}")
            }
          }
          }
        }
    } ~
      path(Segment) { documentationId =>
        get {
          onComplete(docuRepo.getDocumentationById(documentationId)) {
            case Success(Some(user)) => complete(StatusCodes.OK, user)
            case Success(None) => complete(StatusCodes.NotFound, s"Документа под ID $documentationId не существует!")
            case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
          }
        } ~
          put {
            entity(as[DocumentationUpdate]) { updatedDocument => {
              onComplete(docuRepo.updateDocumentation(documentationId,updatedDocument)) {
                case Success(updatedCourseId) =>
                  complete(StatusCodes.OK, updatedCourseId)
                case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
              }
            }
            }
          } ~
          delete {
            onComplete(docuRepo.deleteDocumentation(documentationId)) {
              case Success(deletedDocumentationId) =>
                complete(StatusCodes.OK, s"ID удаленного документа: $deletedDocumentationId")
              case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
            }
          }
      }
  }
}
