import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexes matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*;uk\.gov\.hmrc\.uploaddocuments\.views\.html\.components\.*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "5.20.0",
  "uk.gov.hmrc"                  %% "auth-client"                % "5.8.0-play-28",
  "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % "0.59.0",
  "uk.gov.hmrc"                  %% "crypto"                     % "6.1.0",
  "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "2.0.0-play-28",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.12.5",
  "com.sun.mail"                  % "javax.mail"                 % "1.6.2",
  "org.jsoup"                     % "jsoup"                      % "1.14.3"
)

def testDeps(scope: String) =
  Seq(
    "org.scalatest"       %% "scalatest"       % "3.2.8"   % scope,
    "com.vladsch.flexmark" % "flexmark-all"    % "0.36.8"  % scope,
    "org.scalameta"       %% "munit"           % "0.7.29"  % scope,
    "org.scalacheck"      %% "scalacheck"      % "1.15.4"  % scope,
    "org.scalatestplus"   %% "scalacheck-1-15" % "3.2.8.0" % scope
  )

lazy val itDeps = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"  % "it",
  "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2" % "it"
)

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
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it") ++ itDeps,
    publishingSettings,
    scoverageSettings,
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
