ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "dev.xymox"

val zioVersion     = "1.0.9"
val zioJsonVersion = "0.1.5"
val zioHttpVersion = "1.0.0.0-RC17+12-2f7aa146-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "zio-rest",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % zioVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "io.d11"  %% "zhttp"    % zioHttpVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
