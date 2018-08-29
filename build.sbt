name := "persistence"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "org.apache.ignite" % "ignite-core" % "2.6.0",
  "org.postgresql" % "postgresql" % "42.2.4",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-simple" % "1.7.25"
)

assemblyJarName in assembly := "ignite-persistence.jar"
mainClass := Some("test.nodes.TestNativePersistence")