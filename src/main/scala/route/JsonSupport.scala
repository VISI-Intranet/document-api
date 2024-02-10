package route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import domain._
import spray.json._

import java.text.SimpleDateFormat
import java.util.Date
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  import spray.json._

  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    private val pattern = "yyyy-MM-dd"
    private val formatter = new SimpleDateFormat(pattern)

    def write(obj: Date): JsValue = JsString(formatter.format(obj))

    def read(json: JsValue): Date = json match {
      case JsString(dateString) => formatter.parse(dateString)
      case _ => throw DeserializationException("Неправильные данные для типа Date. Правильный формат: ГГГГ-ММ-ДД!")
    }
  }

  implicit object UserTypeFormat extends RootJsonFormat[Usertype.Usertype] {
    def write(obj: Usertype.Usertype): JsValue = JsString(obj.toString)

    def read(json: JsValue): Usertype.Usertype = json match {
      case JsString(value) => Usertype.withName(value)
      case _ => throw DeserializationException(
        "Неправильные данные для типа UserType. " +
          "Допустимые значения: , " +"STUDENT, ADMIN, TEACHER")
    }
  }

  implicit object DocumentTypeFormat extends RootJsonFormat[Documenttype.Documenttype] {
    def write(obj: Documenttype.Documenttype): JsValue = JsString(obj.toString)

    def read(json: JsValue): Documenttype.Documenttype = json match {
      case JsString(value) => Documenttype.withName(value)
      case _ => throw DeserializationException(
        "Неправильные данные для типа UserType. " +
          "Допустимые значения: , " + "PPTX,DOCX,ZIP,PDF")
    }
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat5(User)
  implicit val userListFormat: RootJsonFormat[List[User]] = listFormat(userFormat)
  implicit val userUpdateFormat: RootJsonFormat[UserUpdate] = jsonFormat4(UserUpdate)


  implicit val courseFormat: RootJsonFormat[Course] = jsonFormat5(Course)
  implicit val courseListFormat: RootJsonFormat[List[Course]] = listFormat(courseFormat)
  implicit val courseUpdateFormat: RootJsonFormat[CourseUpdate] = jsonFormat4(CourseUpdate)


  implicit val documentationFormat: RootJsonFormat[Documentation] = jsonFormat9(Documentation)
  implicit val documentationListFormat: RootJsonFormat[List[Documentation]] = listFormat(documentationFormat)
  implicit val documentationUpdateFormat: RootJsonFormat[DocumentationUpdate] = jsonFormat8(DocumentationUpdate)

}