package Alpakka.Handlers.AddHandler

object RecieveHandler {

  val handler: (String, String) => Unit = (message, routingKey) => {

    routingKey match {
      case "univer.teacher-api.studentsByIdGet" =>
        // Обработка для ключа "key1"
        println(s"Hello fucking shit")
      case "key2" =>
        // Обработка для ключа "key2"
        println(s"Received message for key2: $message")
      case _ =>
        // Обработка для всех остальных случаев
        println(s"Received message with unknown routing key: $routingKey")
    }

  }

}
