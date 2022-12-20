import play.sbt.routes.RoutesKeys
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val root = (project in file("."))
  .settings(
    name := "upload-customs-documents-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    PlayKeys.playDefaultPort := 10110,
    RoutesKeys.routesImport += "uk.gov.hmrc.uploaddocuments.models.JourneyId",
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.{components => hmrcComponents}",
      "uk.gov.hmrc.uploaddocuments.views.html.components",
      "uk.gov.hmrc.uploaddocuments.views.ViewHelpers._"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    publishingSettings,
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
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / javaOptions += "-Djava.locale.providers=CLDR,JRE"
  )
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
