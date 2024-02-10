package repo

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult

import scala.concurrent.{ExecutionContext, Future}
import domain.{Course, CourseUpdate}
import org.mongodb.scala.bson.{BsonArray, BsonDateTime, BsonDocument, BsonInt32, BsonString, ObjectId}

import java.text.SimpleDateFormat
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

class CourseRepository(implicit db:MongoDatabase) {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val collection = db.getCollection("course")

  // Gets
  def getAllCourses()(implicit ec: ExecutionContext): Future[List[Course]] = {
    collection.find().toFuture().map(_.map(docToCourse).collect { case Some(course) => course }.toList)
  }

  // Filter
  def customFilter(field: String, parameter: Any)(implicit ec: ExecutionContext): Future[List[Course]] = {
    collection.find(equal(field, parameter)).toFuture().map(_.map(docToCourse).collect { case Some(course) => course }.toList)
  }

  def getCourseById(courseid: String)(implicit ec: ExecutionContext): Future[Option[Course]] = {
    val objectId = Document("courseid"->courseid.toInt)
    collection.find( objectId).headOption().map(_.flatMap(docToCourse))
  }

  def doesBookExist(courseid: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val objectId = Document("courseid"->courseid.toInt)
    collection.find( objectId).headOption().map(_.isDefined)
  }

  // Create
  def addCourse(course: Course)(implicit ec: ExecutionContext): Future[String] = {
    val document = Document(
      "courseid"->course.courseid,
      "name" -> course.name,
      "userids"->course.userids,
      "enrollmentdate"->course.enrollmentdate
    )

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Курс успешно добавлен с идентификатором: $insertedId"
    }
  }

  // Update
  def updateCourse(courseid: String, updatedCourse: CourseUpdate): Future[String] = {
    val filter = Document("courseid" -> courseid.toInt)
    updatedCourse.toDocument(courseid).flatMap { updatedBson =>
      collection
        .updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult =>
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Course успешно обновлен"
          } else {
            "Обновление Course не выполнено"
          }
        }
    }
  }

  // Delete
  def deleteCourse(courseid: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = Document("courseid"->courseid.toInt)
    collection.deleteOne(objectId).toFuture().map(_ => "Курс успешно удален")
  }


  implicit class RichCourseUpdate(courseUpdate: CourseUpdate) {
    def toDocument(courseid: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      val oldCourseFuture: Future[Option[Course]] = getCourseById(courseid)
      val updatedDocumentFuture: Future[BsonDocument] = oldCourseFuture.flatMap {
        case Some(oldCourse) =>
          val updatedDocument = oldCourse.toDocumentForUpdate(courseUpdate)
          Future.successful(updatedDocument)
        case None =>
          Future.failed(new NoSuchElementException(s"Course with id $courseid not found"))
      }
      updatedDocumentFuture
    }
  }

  implicit class RichCourse(course: Course) {
    def toDocumentForUpdate(update: CourseUpdate): BsonDocument = {
      val document = BsonDocument("$set" -> BsonDocument(
        "courseid" -> BsonInt32(update.courseid.getOrElse(course.courseid)),
        "name" -> BsonString(update.name.getOrElse(course.name)),
        "userids" -> BsonArray(update.userids.getOrElse(course.userids).map(BsonInt32(_))),
        "enrollmentdate" -> BsonDateTime(update.enrollmentdate.map(_.getTime).getOrElse(course.enrollmentdate.getTime))
      ))
      document
    }
  }


  private def docToCourse(doc: Document): Option[Course] = {
    Option(doc).map { d =>
      Course(
        Some(d.getObjectId("_id").toHexString),
        d.getInteger("courseid"),
        d.getString("name"),
        d.getList("userids", classOf[Integer]).asScala.toList.map(_.toInt),
        d.getDate("enrollmentdate")
      )
    }
  }

}
