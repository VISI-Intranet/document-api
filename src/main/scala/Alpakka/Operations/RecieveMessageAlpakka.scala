package Alpakka.Operations

import Alpakka.RabbitMQModel.RabbitMQModel
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.alpakka.amqp.scaladsl.{AmqpSource, CommittableReadResult}
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, AmqpLocalConnectionProvider, NamedQueueSourceSettings, QueueDeclaration}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.ExecutionContext

object RecieveMessageAlpakka {

  def subscription(modelMQ: RabbitMQModel, connectionProvider: AmqpConnectionProvider ,handler: (String, String) => Unit, messageType: String = "Event")
                  (implicit mat: Materializer, ex: ExecutionContext): Unit = {
    val queueName = modelMQ.queueName

    val amqpSource: Source[CommittableReadResult, NotUsed] = AmqpSource.committableSource(
      NamedQueueSourceSettings(connectionProvider, queueName)
        .withDeclaration(QueueDeclaration(queueName).withDurable(true))
        .withAckRequired(true),
      bufferSize = 10
    )

    val processMessage: Flow[CommittableReadResult, Unit, NotUsed] =
      Flow[CommittableReadResult].mapAsync(1) { committableReadResult =>
        val message = new String(committableReadResult.message.bytes.toArray, "UTF-8")
        val routingKey = committableReadResult.message.envelope.getRoutingKey
        val correlationId = committableReadResult.message.properties.getCorrelationId
        println("\n\nrecieve")
        println(s"MessageType:$messageType  Body: $message, CorrelationId: $correlationId, Routing Key: $routingKey $queueName")
        println("recieve\n\n")
        handler(message, routingKey)
        committableReadResult.ack().map(_ => ())
      }

    amqpSource
      .via(processMessage)
      .runWith(Sink.ignore)
  }

}
