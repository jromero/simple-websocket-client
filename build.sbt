name := "simple-websocket-client"

organization := "codes.jromero"

version := "0.1"

scalaVersion := "2.11.8"

//val jarName = SettingKey[String]("jar-name")

//jarName := s"swsc-${version.value}.jar"

// assembly options
mainClass in assembly := Some("codes.jromero.swsc.Main")
assemblyJarName := s"swsc-${version.value}.jar"

// buildinfo options
enablePlugins(BuildInfoPlugin)
buildInfoKeys := Seq[BuildInfoKey](name, version, assemblyJarName)
buildInfoPackage := "codes.jromero.swsc"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.offbytwo" % "docopt" % "0.6.0.20150202"
)