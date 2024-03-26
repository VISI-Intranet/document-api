package repo

import domain.{User, UserUpdate, Usertype}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt32, BsonString, ObjectId}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

class UserRepository(implicit db:MongoDatabase) {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val collection = db.getCollection("user")

  // Gets
  def getAllUsers()(implicit ec: ExecutionContext): Future[List[User]] = {
    collection.find().toFuture().map(_.map(docToUser).collect { case Some(user) => user }.toList)
  }

  // Filter
  def customFilter(field: String, parameter: Any)(implicit ec: ExecutionContext): Future[List[User]] = {
    collection.find(equal(field, parameter)).toFuture().map(_.map(docToUser).collect { case Some(user) => user }.toList)
  }

  def getUserById(userid: String)(implicit ec: ExecutionContext): Future[Option[User]] = {
    val objectId = Document("userid"->userid.toInt)
    collection.find(objectId).headOption().map(_.flatMap(docToUser))
  }

  def doesBookExist(userid: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val objectId = Document("userid"->userid.toInt)
    collection.find( objectId).headOption().map(_.isDefined)
  }

  // Create
  def addUser(user: User)(implicit ec: ExecutionContext): Future[String] = {
    val document = Document(
      "userid"->BsonInt32(user.userid),
      "name" -> user.name,
      "lastname" -> user.lastname,
      "userType" -> user.usertype.toString
    )

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Пользователь успешно добавлен с идентификатором: $insertedId"
    }
  }

  def updateUser(userid: String, updatedUser: UserUpdate): Future[String] = {
    val filter = Document("userid" -> userid.toInt)
    updatedUser.toDocument(userid).flatMap { updatedBson =>

      collection
        .updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult =>
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Пользователь успешно обновлен"
          } else {
            "Обновление пользователя не выполнено"
          }
        }
    }
  }

  // To convert UserUpdate to BsonDocument for update
  implicit class RichUserUpdate(userUpdate: UserUpdate) {
    def toDocument(userid: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      val oldUserFuture: Future[Option[User]] = getUserById(userid)
      val updatedDocumentFuture: Future[BsonDocument] = oldUserFuture.flatMap {
        case Some(oldUser) =>
          val updatedDocument = oldUser.toDocumentForUpdate(userUpdate)
          Future.successful(updatedDocument)
        case None =>
          Future.failed(new NoSuchElementException(s"User with id $userid not found"))
      }
      updatedDocumentFuture
    }
  }

  // To convert User to BsonDocument for update
  implicit class RichUser(user: User) {
    def toDocumentForUpdate(update: UserUpdate): BsonDocument = {
      val document = BsonDocument("$set" -> BsonDocument(
        "userid" -> BsonInt32(update.userid.getOrElse(user.userid)),
        "name" -> BsonString(update.name.getOrElse(user.name)),
        "lastname" -> BsonString(update.name.getOrElse(user.lastname)),
        "usertype" -> BsonString(update.usertype.map(_.toString).getOrElse(user.usertype.toString))
      ))
      document
    }
  }


  // Delete
  def deleteUser(userid: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = Document("userid"->userid.toInt)
    collection.deleteOne( objectId).toFuture().map(_ => "Пользователь успешно удален")
  }

  private def docToUser(doc: Document): Option[User] = {
    Option(doc).map { d =>
      User(
        Some(d.getObjectId("_id").toHexString),
        d.getInteger("userid"),
        d.getString("name"),
        d.getString("lastname"),
        Usertype.withName(d.getString("userType"))
      )
    }
  }
}

