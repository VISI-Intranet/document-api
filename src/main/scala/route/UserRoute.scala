package route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import domain._
import repo.UserRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserRoute(implicit val userRepo: UserRepository, val ex:ExecutionContext)
  extends JsonSupport {
  private val fields: List[String] = List(
    "userid",
    "name",
    "lastname",
    "usertype"
  )

  val route: Route = pathPrefix("user") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          validate(fields.contains(field),
            s"Вы ввели неправильное имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}") {
            val convertedParameter = if (parameter.matches("-?\\d+")) parameter.toInt else parameter
            onComplete(userRepo.customFilter(field, convertedParameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось сделать запрос! ${ex.getMessage}")
            }
          }
        }
      } ~
        get {
          onComplete(userRepo.getAllUsers()) {
            case Success(result) =>
              val userList = result
              complete(StatusCodes.OK, userList)
            case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
          }
        } ~
        post {
          entity(as[User]) { newUser => {
            onComplete(userRepo.addUser(newUser)) {
              case Success(newUserId) =>
                complete(StatusCodes.Created, s"ID нового пользователя $newUserId")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось создать пользователя: ${ex.getMessage}")
            }
          }
          }
        }
    } ~
        path(Segment) { userId =>
          get {
            onComplete(userRepo.getUserById(userId)) {
              case Success(Some(user)) => complete(StatusCodes.OK, user)
              case Success(None) => complete(StatusCodes.NotFound, s"Пользователя под ID $userId не существует!")
              case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
            }
          } ~
          put {
            entity(as[UserUpdate]) { updatedUser => {
              onComplete(userRepo.updateUser(userId, updatedUser)) {
                case Success(updatedUserId) =>
                  complete(StatusCodes.OK, updatedUserId)
                case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
              }
            }
            }
          } ~
          delete {
            onComplete(userRepo.deleteUser(userId)) {
              case Success(deletedUserId) =>
                complete(StatusCodes.OK, s"ID удаленного пользователя: $deletedUserId")
              case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
            }
          }
        }
    }
  }

