import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.*

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val root = (project in file("."))
  .settings(
    name := "upload-customs-documents-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "3.3.6",
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
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / javaOptions += "-Djava.locale.providers=CLDR,JRE"
  )
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

