package Alpakka.Operations

import Alpakka.RabbitMQModel.RabbitMQModel
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.alpakka.amqp._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.rabbitmq.client.AMQP.BasicProperties

import scala.concurrent.{ExecutionContext, Future}

object SendMessageAndWaitForResponsAlpakka {

  def sendMessageAndWaitForResponse(message: String, pubModelMQ: RabbitMQModel, replyModelMQ: RabbitMQModel,amqpConnectionProvider: AmqpConnectionProvider, messageType: String = "Request")
                                   (responseHandler: (String, String) => String = (b, s) => b)
                                   (implicit system: ActorSystem, mat: Materializer, ex: ExecutionContext): Future[String] = {

    val correlationId = java.util.UUID.randomUUID().toString
    val replyQueueName = replyModelMQ.queueName
    val pubExchange = pubModelMQ.exchangeName
    val pubRoutingKey = pubModelMQ.routingKeyName
    val amqpWriteSettings = AmqpWriteSettings(amqpConnectionProvider)
      .withExchange(pubExchange)
      .withRoutingKey(pubRoutingKey)

    val properties = new BasicProperties.Builder()
      .correlationId(correlationId).contentType("Event")
      .replyTo(replyQueueName)
      .build()

    val amqpSink = AmqpSink.apply(amqpWriteSettings)
      .contramap[WriteMessage](writeMessage => writeMessage.withProperties(properties))

    val writing: Future[Unit] =
      Source.single(WriteMessage(ByteString(message)))
        .runWith(amqpSink)
        .map { _ =>
          println(s"\nMessage sentWait successfully\n\n ")
        }
        .recover {
          case e: Throwable =>
            println(s"Error sending message")
        }


    val response: Future[String] = writing.flatMap { _ =>
      AmqpSource
        .atMostOnceSource(
          NamedQueueSourceSettings(AmqpLocalConnectionProvider, replyQueueName)
            .withDeclaration(QueueDeclaration(replyQueueName).withDurable(true))
            .withAckRequired(true),
          bufferSize = 1
        )
        .map { envelope =>
          val routingKey = envelope.envelope.getRoutingKey
          val response = envelope.bytes.utf8String
          println("\n\nSendTakeWait")
          println(s"Received response for correlationId: $correlationId - message: $response RK - $routingKey")
          println("\n\n")
          responseHandler(response, routingKey)
        }
        .take(1)
        .runWith(Sink.head)
        .recover {
          case e: Throwable =>
            println(s"Error receiving response for correlationId: $correlationId - ${e.getMessage}")
            throw e
        }
    }

    response
  }

}
