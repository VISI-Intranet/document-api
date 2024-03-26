package scala
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

object MongoConnection {
  private val mongoClient = MongoClient("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("univer")
  val userCollection: MongoCollection[Document] = database.getCollection("user")
  val documentationCollection: MongoCollection[Document] = database.getCollection("documentation")
  val courseCollection: MongoCollection[Document] = database.getCollection("course")
}
