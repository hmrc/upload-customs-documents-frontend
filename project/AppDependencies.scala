import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30" % "9.12.0",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % "2.6.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30" % "12.1.0",
    "com.sun.mail"                  % "javax.mail"                 % "1.6.2",
    "org.jsoup"                     % "jsoup"                      % "1.20.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.17.1"
  )

  val test = Seq(
    "org.scalamock"          %% "scalamock"          % "6.0.0"    % "test",
    "org.scalatest"          %% "scalatest"          % "3.2.19"   % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.64.8"   % "test, it",
    "org.scalameta"          %% "munit-diff"         % "1.0.1"    % "test, it",
    "org.scalacheck"         %% "scalacheck"         % "1.18.1"   % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-18"    % "3.2.19.0" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "3.0.1"    % "it"
  )
}
