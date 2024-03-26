package repo

import org.mongodb.scala.{MongoDatabase, documentToUntypedDocument}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.model.Updates._

import scala.concurrent.{ExecutionContext, Future}
import domain.{Documentation, DocumentationUpdate, Documenttype}
import org.mongodb.scala.bson.{BsonDateTime, BsonDocument, BsonInt32, BsonObjectId, BsonString, ObjectId}
import org.mongodb.scala.model.Updates.{combine, set}


class DocumentationRepository(implicit db: MongoDatabase) {

  private val collection = db.getCollection("documentation")

  def getAllDocumentation()(implicit ec: ExecutionContext): Future[List[Documentation]] = {
    collection.find().toFuture().map(_.map(docToDocumentation).collect { case Some(documentation) => documentation }.toList)
  }

  def customFilter(field: String, parameter: Any)(implicit ec: ExecutionContext): Future[List[Documentation]] = {
    collection.find(equal(field, parameter)).toFuture().map(_.map(docToDocumentation).collect { case Some(documentation) => documentation }.toList)
  }

  def getDocumentationById(documentationId: String)(implicit ec: ExecutionContext): Future[Option[Documentation]] = {
    val objectId = Document("docid"->documentationId.toInt)
    collection.find(objectId).headOption().map(_.flatMap(docToDocumentation))
  }

  def doesDocumentationExist(documentationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val objectId = Document("docid"->documentationId.toInt)
    collection.find(objectId).headOption().map(_.isDefined)
  }

  def addDocumentation(documentation: Documentation)(implicit ec: ExecutionContext): Future[String] = {
    val document = Document(
      "docid"->documentation.docid,
      "title" -> documentation.title,
      "userid" -> documentation.userid,
      "courseid" -> documentation.courseid,
      "dateuploaded" -> documentation.dateuploaded,
      "documentType" -> documentation.documenttype.toString,
      "tags" -> documentation.tags,
      "fileURL" -> documentation.fileURL
    )

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Документ успешно добавлен с идентификатором: $insertedId"
    }
  }


  def updateDocumentation(docid: String, documentationUpdated: DocumentationUpdate)(implicit ec: ExecutionContext):
  Future[String] = {
    val filter = Document("docid"->docid.toInt)
    documentationUpdated.toDocument(docid).flatMap{updatedBson=>
      collection.updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult => {
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Документ успешно обновлен"
          } else {
            "Обновление документа не выполнено"
          }
        }
        }
    }
  }


  implicit class RichDocumentationUpdate(docUpdate: DocumentationUpdate) {
    def toDocument(docid: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      val oldDocFuture: Future[Option[Documentation]] = getDocumentationById(docid)
      val updatedDocumentFuture: Future[BsonDocument] = oldDocFuture.flatMap {
        case Some(oldDoc) =>
          val updatedDocument = oldDoc.toDocumentForUpdate(docUpdate)
          Future.successful(updatedDocument)
        case None =>
          Future.failed(new NoSuchElementException(s"Document with id $docid not found"))
      }
      updatedDocumentFuture
    }
  }

  implicit class RichDocumentation(documentation: Documentation) {
    def toDocumentForUpdate(update: DocumentationUpdate): BsonDocument = {
      val document = BsonDocument("$set" -> BsonDocument(
        "docid" -> BsonInt32(update.docid.getOrElse(documentation.docid)),
        "title" -> BsonString(update.title.getOrElse(documentation.title)),
        "userid" -> BsonInt32(update.userid.getOrElse(documentation.userid)),
        "courseid" -> BsonInt32(update.courseid.getOrElse(documentation.courseid)),
        "dateuploaded" -> BsonDateTime(update.dateuploaded.map(_.getTime).getOrElse(documentation.dateuploaded.getTime)),
        "documenttype" -> BsonString(update.documenttype.map(_.toString).getOrElse(documentation.documenttype.toString)),
        "tags" -> BsonString(update.tags.getOrElse(documentation.tags)),
        "fileURL" -> BsonString(update.fileURL.getOrElse(documentation.fileURL))
      ))
      document
    }
  }



  // Delete
  def deleteDocumentation(documentationId: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = Document("docid"->documentationId.toInt)
    collection.deleteOne(objectId).toFuture().map(_ => "Documentation successfully deleted")
  }

  private def docToDocumentation(doc: Document): Option[Documentation] = {
    Option(doc).map { d =>
      Documentation(
        Some(d.getObjectId("_id").toHexString),
        d.getInteger("docid"),
        d.getString("title"),
        d.getInteger("userid"),
        d.getInteger("courseid"),
        d.getDate("dateuploaded"),
        Documenttype.withName(d.getString("documentType")),
        d.getString("tags"),
        d.getString("fileURL"))
    }
  }
}


