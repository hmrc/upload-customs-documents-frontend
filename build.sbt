import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val root = (project in file("."))
  .settings(
    name := "upload-customs-documents-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    PlayKeys.playDefaultPort := 10100,
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
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
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
    )
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / scalafmtOnCompile := true,
    IntegrationTest / javaOptions += "-Djava.locale.providers=CLDR,JRE"
  )
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
