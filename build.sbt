ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.10"

lazy val zioRedisDependencies = Seq(
  "dev.zio" %% "zio-redis"           % "1.1.10",
  "dev.zio" %% "zio-schema-protobuf" % "1.6.1",
  "dev.zio" %% "zio-schema-json"     % "1.8.5"
)

lazy val driverDependencies = Seq(
  "ch.qos.logback" % "logback-classic"    % "1.3.10",
  "dev.zio"        %% "zio-logging-slf4j" % "2.1.13"
)

lazy val root = (project in file("."))
  .settings(
    name := "pet-project-zio-redis",
    organization := "ru.krivitskaya.anna",
    libraryDependencies ++= Seq(
      zioRedisDependencies,
      driverDependencies
    ).reduce(_ ++ _),
    javacOptions ++= Seq("-source", "1.8"),
    compileOrder := CompileOrder.JavaThenScala
  )
