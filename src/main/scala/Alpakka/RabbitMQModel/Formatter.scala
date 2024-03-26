package RabbitMQ.RabbitMQOperation.Operations

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object Formatter {

  def extractContentList[T: ClassTag](obj: T): String = {
    val value = obj match {
      case Some(lst: List[_]) => lst.map(_.toString).mkString(",")
      case lst: List[_] => lst.map(_.toString).mkString(",")
      case _ => ""
    }
    value
  }

  def extractContent[T: ClassTag](obj: T): String = {
    val fields = obj.getClass.getDeclaredFields.map(_.getName)
    val values = fields.map { fieldName =>
      val field = obj.getClass.getDeclaredField(fieldName)
      field.setAccessible(true)
      val value = field.get(obj)
      val cleanedValue = value match {
        case Some(v) => v.toString
        case other => other.toString
      }
      cleanedValue
    }
    values.mkString(",")
  }

  object StringToObjectConverter {
    // Оставляем только один метод stringToObject
    def stringToObject[T](str: String)(implicit classTag: ClassTag[T]): T = {
      val clazz = implicitly[ClassTag[T]].runtimeClass
      clazz.getDeclaredConstructor(classOf[String]).newInstance(str).asInstanceOf[T]
    }

    // Метод, который принимает Future[String] и возвращает Future[T]
    def processResult[T](futureString: Future[String])(implicit classTag: ClassTag[T], ex: ExecutionContext): Future[T] = {
      futureString.map(stringToObject[T])
    }

  }
}