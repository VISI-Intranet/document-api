package Alpakka.Operations

import Alpakka.RabbitMQModel.RabbitMQModel
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, AmqpLocalConnectionProvider, AmqpWriteSettings, WriteMessage}
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.rabbitmq.client.AMQP.BasicProperties

import scala.concurrent.{ExecutionContext, Future}

object SendMessageWithCorrelationIdAlpakka {

  def sendMessageWithCorrelationId[T](message: T, modelMQ: RabbitMQModel,amqpConnectionProvider: AmqpConnectionProvider , correlationIdArg: String = "", messageType: String = "Event")
                                     (handler: (T,String) => String = (msg: T, routingKey:String) => msg.toString)
                                     (implicit system: ActorSystem, mat: Materializer, ex: ExecutionContext): Future[Unit] = {

    var correlationId = correlationIdArg;

    val exchange = modelMQ.exchangeName
    val routingKey = modelMQ.routingKeyName

    if (correlationId.isEmpty)
      correlationId = java.util.UUID.randomUUID().toString

    val amqpWriteSettings = AmqpWriteSettings(amqpConnectionProvider)
      .withExchange(exchange)
      .withRoutingKey(routingKey)

    val modifiedMessage = handler(message , routingKey)

    val properties = new BasicProperties.Builder().correlationId(correlationId).build()

    val amqpSink = AmqpSink.apply(amqpWriteSettings)
      .contramap[WriteMessage](writeMessage => writeMessage.withProperties(properties))

    // Отправляем преобразованное сообщение
    val writing: Future[Unit] =
      Source.single(WriteMessage(ByteString(modifiedMessage)))
        .runWith(amqpSink)
        .map(_ => ())

    writing.foreach { _ =>
      println("\n\nsend")
      println(s"Message Type: $messageType")
      println(s"Message Body: $modifiedMessage")
      println("send\n\n")
    }

    writing
  }

}
