name := "TchooMultiCores"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"
libraryDependencies += "com.pi4j" % "pi4j-core" % "1.1"
libraryDependencies += "info.cukes" %% "cucumber-scala" % "1.2.5" % "test"
libraryDependencies += "info.cukes" % "cucumber-junit" % "1.2.5" % "test"
libraryDependencies += "junit" % "junit" % "4.12" % "test"

scalacOptions += "-feature"
