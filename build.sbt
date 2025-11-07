import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"

val appName = "upload-customs-documents-frontend"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val root = (project in file("."))
  .settings(
    name := "upload-customs-documents-frontend",
    organization := "uk.gov.hmrc",
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
    WebpackKeys.configurations := Seq(
      WebpackConfig(
        id = "js",
        configFilePath = "webpack.javascript.config.js",
        includeFilter = "*.js" || "*.ts",
        inputs = Seq("javascripts/index.ts"),
        output = "javascripts/application.min.js"
      ),
      WebpackConfig(
        id = "css",
        configFilePath = "webpack.stylesheet.config.js",
        includeFilter = "*.scss" || "*.sass" || "*.css",
        inputs = Seq("stylesheets/application.scss"),
        output = "stylesheets/application.css"
      ),
      WebpackConfig(
        id = "print",
        configFilePath = "webpack.stylesheet.config.js",
        includeFilter = "*.scss" || "*.sass" || "*.css",
        inputs = Seq("stylesheets/print.scss"),
        output = "stylesheets/print.css"
      )
    ),
    scalacOptions += s"-Wconf:src=${target.value}/scala-${scalaBinaryVersion.value}/routes/.*:s,src=${target.value}/scala-${scalaBinaryVersion.value}/twirl/.*:s"
  )
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)


lazy val it = (project in file("it"))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(CodeCoverageSettings.settings*)
  .settings(
    publish / skip := true,
    Test / testOptions += Tests.Argument("-o", "-h", "it/target/html-report")
  )

addCommandAlias("runAllChecks", "clean;compile;scalafmtAll;coverage;test;it/test;coverageReport")