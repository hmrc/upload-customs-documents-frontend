import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.*

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    name := "upload-customs-documents-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "3.3.7",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    PlayKeys.playDefaultPort := 10110,
    RoutesKeys.routesImport += "uk.gov.hmrc.uploaddocuments.models.JourneyId",
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    CodeCoverageSettings.settings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    majorVersion := 0,
    Test / javaOptions += "-Djava.locale.providers=CLDR,JRE",
    scalacOptions ++= Seq(
      s"-Wconf:src=${target.value}/scala-${scalaBinaryVersion.value}/routes/.*:s",
      s"-Wconf:src=${target.value}/scala-${scalaBinaryVersion.value}/twirl/.*:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:src=.*twirl.*&msg=unused import:s",
      "-Wconf:msg=Flag.*repeatedly:s"
    )
  )

// Run with `sbt it/test`
lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin)
  .dependsOn(root % "test->test")
  .settings(
    majorVersion := 0,
    scalaVersion := "3.3.7",
    Test / scalaSource := baseDirectory.value,
    Test / parallelExecution := false,
    Test / fork := false,
    Test / javaOptions += "-Djava.locale.providers=CLDR,JRE"
  )
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

