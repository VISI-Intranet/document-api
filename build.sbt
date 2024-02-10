ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "document-db",
    libraryDependencies ++=Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0"
    ))
