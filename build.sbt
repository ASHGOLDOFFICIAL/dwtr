inThisBuild {
  List(
    version           := "0.1.0-SNAPSHOT",
    scalaVersion      := "3.3.6",
    semanticdbEnabled := true
  )
}


lazy val root = (project in file("."))
  .settings(
    name             := "dwtr",
    idePackagePrefix := Some("org.aulune")
  )


scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Wnonunit-statement"
)


val argon2Version     = "2.12"
val catsEffectVersion = "3.6.3"
val circeVersion      = "0.14.14"
val doobieVersion     = "1.0.0-RC9"
val http4sVersion     = "0.23.30"
val jwtVersion        = "11.0.2"
val log4catsVersion   = "2.7.1"
val logbackVersion    = "1.5.18"
val pureconfigVersion = "0.17.9"
val sqliteVersion     = "3.50.3.0"
val tapirVersion      = "1.11.40"

resolvers += Resolver.sonatypeCentralSnapshots


libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-client",
  "org.http4s" %% "http4s-ember-server",
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-circe"
).map(_ % http4sVersion)


libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"
).map(_ % tapirVersion)


libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari"
).map(_ % doobieVersion)


libraryDependencies ++= Seq(
  "org.xerial"     % "sqlite-jdbc"    % sqliteVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
  "ch.qos.logback"         % "logback-classic" % logbackVersion,
  "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,
  "com.github.jwt-scala"  %% "jwt-circe"       % jwtVersion,
  "de.mkammerer"           % "argon2-jvm"      % argon2Version
)
