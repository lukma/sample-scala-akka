
name := "rockid-collection-service"

version := "0.0.1"

organization := "i0.rockid"

scalaVersion := "2.12.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "10.0.7"
  val slickV = "3.2.0"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaV,

    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",

    "org.slf4j" % "slf4j-nop" % "1.7.25",
    "joda-time" % "joda-time" % "2.9.9",
    "org.joda" % "joda-convert" % "1.8.1",
    "com.nulab-inc" %% "scala-oauth2-core" % "1.3.0",
    "ch.megard" %% "akka-http-cors" % "0.2.1"
  )
}

mainClass in Global := Some("Boot")

fork := true
