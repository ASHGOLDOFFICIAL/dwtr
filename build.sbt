import sbtassembly.MergeStrategy


inThisBuild {
  List(
    idePackagePrefix := Some("org.aulune"),
    name := "dwtr",
    organization := "org.aulune",
    scalaVersion := "3.3.6",
    semanticdbEnabled := true,
    version := "0.1.0-SNAPSHOT",
  )
}


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList(
         "META-INF",
         "maven",
         "org.webjars",
         "swagger-ui",
         "pom.properties") => MergeStrategy.singleOrError
  case PathList("META-INF", "resources", "webjars", "swagger-ui", _*) =>
    MergeStrategy.singleOrError
  case PathList("META-INF", _*) => MergeStrategy.discard

  case "module-info.class" => MergeStrategy.discard
  case x                   => (assembly / assemblyMergeStrategy).value.apply(x)
}


lazy val root = (project in file(".")).aggregate(core, integration)


lazy val core = (project in file("core"))
  .settings(
    assembly / mainClass := Some("org.aulune.App"),
    name := "core",
    libraryDependencies ++= http4sDeps ++ tapirDeps ++ circeDeps ++ doobieDeps ++ Seq(
      "ch.qos.logback"         % "logback-classic" % logbackVersion,
      "com.github.jwt-scala"  %% "jwt-circe"       % jwtVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,
      "de.mkammerer"           % "argon2-jvm"      % argon2Version,
      "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
      "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "org.xerial"     % "sqlite-jdbc"    % sqliteVersion,
    ),
  )


lazy val integration = (project in file("integration"))
  .dependsOn(core)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersVersion % Test,
    ),
  )


scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Wnonunit-statement",
)


val argon2Version = "2.12"
val catsEffectTestingVersion = "1.6.0"
val catsEffectVersion = "3.6.3"
val catsVersion = "2.13.0"
val circeVersion = "0.14.14"
val doobieVersion = "1.0.0-RC9"
val http4sVersion = "0.23.30"
val jwtVersion = "11.0.2"
val log4catsVersion = "2.7.1"
val logbackVersion = "1.5.18"
val pureconfigVersion = "0.17.9"
val scalatestVersion = "3.2.19"
val sqliteVersion = "3.50.3.0"
val tapirVersion = "1.11.40"
val testcontainersVersion = "0.43.0"

resolvers += Resolver.sonatypeCentralSnapshots


val http4sDeps = Seq(
  "org.http4s" %% "http4s-ember-client",
  "org.http4s" %% "http4s-ember-server",
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-circe",
).map(_ % http4sVersion)


val circeDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)


val tapirDeps = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle",
).map(_ % tapirVersion)


val doobieDeps = Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari",
  "org.tpolecat" %% "doobie-postgres",
).map(_ % doobieVersion)


libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectTestingVersion % Test,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
)
